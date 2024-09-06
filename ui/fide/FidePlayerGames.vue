<template>
  <div>
    <!-- Other content of the page -->
    <button @click="downloadGames" class="button">Download Games</button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import axios from 'axios';

export default defineComponent({
  name: 'FidePlayerGames',
  props: {
    playerId: {
      type: String,
      required: true,
    },
  },
  methods: {
    async downloadGames() {
      try {
        const response = await axios.get(`/api/fide/${this.playerId}/games`, {
          responseType: 'blob',
        });
        const blob = new Blob([response.data], { type: 'application/x-chess-pgn' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `${this.playerId}_games.pgn`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      } catch (error) {
        console.error('Error downloading games:', error);
      }
    },
  },
});
</script>

<style scoped>
.button {
  margin-top: 10px;
  padding: 10px 20px;
  background-color: #4caf50;
  color: white;
  border: none;
  cursor: pointer;
}

.button:hover {
  background-color: #45a049;
}
</style>

<template>
  <div>
    <!-- Other content of the page -->
    <button @click="shareGames" class="button">Share Games</button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'FidePlayerGames',
  props: {
    playerId: {
      type: String,
      required: true,
    },
  },
  methods: {
    shareGames() {
      const url = `${window.location.origin}/fide/${this.playerId}`;
      if (navigator.share) {
        navigator
          .share({
            title: `Games of ${this.playerId}`,
            url: url,
          })
          .catch(console.error);
      } else {
        navigator.clipboard.writeText(url).then(
          () => {
            alert('Link copied to clipboard!');
          },
          error => {
            console.error('Error copying link:', error);
          },
        );
      }
    },
  },
});
</script>
