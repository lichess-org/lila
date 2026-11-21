const assert = require('node:assert/strict');
const { test } = require('node:test');

const { hasAiDisclosure, hasVisualProof, run } = require('./ai-policy');

function mockGithub(commitMessages, comments = []) {
  const calls = [];
  const github = {
    rest: {
      pulls: { listCommits: 'listCommits' },
      issues: {
        listComments: 'listComments',
        createComment: async args => calls.push(['comment', args]),
        deleteComment: async args => calls.push(['deleteComment', args]),
        addLabels: async args => calls.push(['label', args]),
      },
    },
    paginate: async method => {
      if (method === github.rest.pulls.listCommits) {
        return commitMessages.map(message => ({ commit: { message } }));
      }

      return comments;
    },
    request: async (route, args) => calls.push(['review', route, args]),
  };

  return { github, calls };
}

test('detects common AI tool disclosures', () => {
  for (const content of [
    '# AI disclosure',
    '# AI assistance',
    '# AI Usage',
    'Used GitHub Copilot to write the tests.',
    'Claude Code was used to implement the change.',
    'AI tool: Cursor',
    'AI-assisted with OpenCode.',
    'AI-assisted with Devin (GLM-5.2 High).',
    'IA: Used Zed/GPT-6 Luna and after Zed/GPT-6.1 Terra to make code changes.',
    'OpenAI Codex assisted with code research, implementation, automated checks and PR preparation.',
    '🤖 Generated with Claude Code',
    'Model: Opus 5.5 Medium',
  ]) {
    assert.equal(hasAiDisclosure(content), true, content);
  }
});

test('does not mistake the PR template for an AI disclosure', () => {
  assert.equal(
    hasAiDisclosure('Include the AI tool you used. Rules about AI-assisted contributions.'),
    false,
  );
});

test('detects image and video proof', () => {
  for (const content of [
    '![Screenshot](https://example.com/screenshot.png)',
    'https://github.com/user-attachments/assets/abc123',
    '<video src="https://example.com/demo.mp4"></video>',
  ]) {
    assert.equal(hasVisualProof(content), true, content);
  }
});

test('labels disclosed AI commits, requests Copilot review, and asks for missing proof', async () => {
  const { github, calls } = mockGithub(['Used Cursor to generate the test.']);
  const context = {
    repo: { owner: 'lichess-org', repo: 'lila' },
    payload: { action: 'opened', pull_request: { number: 42, body: '', labels: [] } },
  };

  await run(github, context, { info: () => {} });

  assert.deepEqual(
    calls.map(([type]) => type),
    ['comment', 'label', 'review'],
  );
});

test('does not ask for proof when the PR description contains an image', async () => {
  const { github, calls } = mockGithub([]);
  const context = {
    repo: { owner: 'lichess-org', repo: 'lila' },
    payload: {
      pull_request: {
        number: 42,
        body: '![Screenshot](https://github.com/user-attachments/assets/abc123)',
        labels: [],
      },
    },
  };

  await run(github, context, { info: () => {} });

  assert.deepEqual(calls, []);
});

test('deletes the bot proof request after the PR description gains an image', async () => {
  const marker = '<!-- lila-ai-policy-visual-proof -->';
  const { github, calls } = mockGithub(
    [],
    [
      {
        id: 123,
        user: { login: 'github-actions[bot]' },
        body: `${marker}\nPlease add visual proof.`,
      },
    ],
  );
  const context = {
    repo: { owner: 'lichess-org', repo: 'lila' },
    payload: {
      action: 'edited',
      pull_request: {
        number: 42,
        body: '![Screenshot](https://github.com/user-attachments/assets/abc123)',
        labels: [],
      },
    },
  };

  await run(github, context, { info: () => {} });

  assert.deepEqual(calls, [['deleteComment', { owner: 'lichess-org', repo: 'lila', comment_id: 123 }]]);
});
