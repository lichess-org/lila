let members = db.team_member
    .find({ team: 'streamers-union' })
    .toArray()
    .map((member) => member.user)

let frontPageStreamers = db.streamer
    .find({ 'approval.tier': { $gt: 0 } })
    .sort({ 'approval.tier': -1 })
    .toArray()
    .map((streamer) => {
        return {
            user: streamer._id,
            tier: streamer.approval.tier,
        }
    })

print('=============================')
print('Streamers Union member report')
print('=============================')

print(`Streamers Union members: ${members.length}`)
print(`Frontpage streamers (tier > 0): ${frontPageStreamers.length}`)

print('---')

print('Frontpage streamers who are not in Streamers Union group (tier in parens):')
print('Should any of these be added to the group?')
print("    " + frontPageStreamers.filter((s) => !members.includes(s.user)).map((s) => `${s.user} (${s.tier})`).join("\n    "))

print('---')

print('Streamers Union members who are not frontpage streamers:')
print('Should any of these be removed from the group?')
print("    " + members.filter((id) => !frontPageStreamers.map((s) => s.user).includes(id)).join("\n    "))
