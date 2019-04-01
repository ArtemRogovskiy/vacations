import Vuex from 'vuex'
import Vue from 'vue'
import {instance} from "../Api";

Vue.use(Vuex);

export default new Vuex.Store({

    state: {
        token: localStorage.getItem('token') || '',
        status: '',
        loader: false

    },

    mutations: {
        authSuccess(state, token) {
            state.token = token;
            state.status = 'success';
        },

        authError(state) {
            state.token = '';
            state.status = 'error';
        },

        authLogout(state) {
            state.token = ''
        }
    },

    actions: {
        login(context, payload) {
            return new Promise((resolve, reject) => {
                let data = {
                    username: payload.username,
                    password: payload.password
                }
                instance.post('/login', data)
                    .then((response) => {
                        console.log(response);
                        let accessToken = response.headers['authorization'];
                        console.log(accessToken);
                        context.commit('authSuccess', accessToken);
                        localStorage.setItem('token', accessToken);
                        instance.defaults.headers.common['Authorization'] = 'Bearer ' + accessToken;
                        resolve(response);
                    })
                    .catch((error) => {
                        localStorage.removeItem('token');
                        context.commit('authError');
                        console.log(error);
                        reject(error);
                    })

            })

        },

    },


    getters: {
        isAuthenticated: state => !!state.token,
        authStatus: state => state.status,
        menus: (state, getters) => {
            if (getters.isAuthenticated) {
                return [{
                    name: "Logout", route: "Logout"
                }]
            }
            return [
                {name: "Login", route: "Login"},
            ];
        }
    }

})