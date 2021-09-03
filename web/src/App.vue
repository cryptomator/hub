<template>
	<Navbar v-show="showNavbar" />
  <ul>
    <li><router-link to="/">Home</router-link></li>
    <li><router-link to="/vaults/create">Create vault</router-link></li>
    <li><router-link to="/user">User Details</router-link></li>
    <li><router-link to="/logout">Logout</router-link></li>
  </ul>

  <router-view/>
</template>

<script>
import Navbar from './components/Navbar.vue';

export default {
	name: 'App',
	components: {
		Navbar,
		//Footer,
	},
	computed: {
		showNavbar() {
			return this.$route.meta.navbar == null ? true : this.$route.meta.navbar;
		},
		/*showFooter() {
			return this.$route.meta.footer == null ? true : this.$route.meta.footer;
		},*/
	},
};
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 0px;
}
</style>
