<template>
  <ul>
    <li><router-link to="/">Home</router-link></li>
    <li><router-link to="/other">Other</router-link></li>
  </ul>

  <router-view/>
</template>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
