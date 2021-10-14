<template>
  <Navbar v-show="showNavbar" />

  <div class="my-5 max-w-7xl mx-auto sm:px-6 lg:px-8">
    <router-view></router-view>
  </div>
</template>

<script>
import Navbar from './components/Navbar.vue';
import VaultList from "./components/VaultList.vue";
import { useI18n } from 'vue-i18n'


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
	setup() {
  	const { t } = useI18n({
  	  useScope: 'global'
  	})
  	return { t }
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
