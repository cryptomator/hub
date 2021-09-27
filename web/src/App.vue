<template>
  <Navbar v-show="showNavbar" />

  <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
    <ul>
      <li><router-link to="/">Home</router-link></li>
      <li><router-link to="/vaults/create">Create vault</router-link></li>
      <li><router-link to="/user">User Details</router-link></li>
      <li><router-link to="/logout">Logout</router-link></li>
    </ul>
    <router-view></router-view>
</div>

</template>

<script>
import Navbar from './components/Navbar.vue';
import VaultList from "./components/VaultList.vue";

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

  /*font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 0px;*/
}
</style>
