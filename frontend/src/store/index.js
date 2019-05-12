import Vuex from 'vuex'
import Vue from 'vue'
import axios from 'axios'
import {instance} from '../Api'
import jwt from 'jsonwebtoken'

Vue.use(Vuex);

const store = new Vuex.Store({

    state: {
        token: localStorage.getItem('token') || '',
        isAuth: false,
        userTeam: {
            teamId: 0,
            name: ''
        },
        team_msg: ''
    },

    mutations: {
        authSuccess(state, token) {
            state.token = token;
            state.isAuth = true;
        },

        teamSuccess(state, team) {
            state.userTeam = team;
        },

        teamFailure(state, msg) {
            state.team_msg = msg;
        },

        authLogout(state) {
            state.token = '';
            state.isAuth = false;
            state.userTeam.teamId = 0;
            state.userTeam.name = '';

        }
    },

    actions: {
        login(context, payload) {
            return new Promise((resolve, reject) => {
                axios.post('/login', payload)
                    .then((response) => {
                        let accessToken = response.headers['authorization'];
                        context.commit('authSuccess', accessToken);
                        localStorage.setItem('token', accessToken);
                        instance.defaults.headers.Authorization = accessToken;
                        let ca = accessToken.substring(7);
                        let decodedValue = jwt.decode(ca, {algorithm: 'HS512'});
                        localStorage.setItem('user', decodedValue.authorities);
                        localStorage.setItem('username', decodedValue.sub);
                        resolve(response);
                    })
                    .catch((error) => {
                        localStorage.removeItem('token');
                        reject(error);
                    })

            })
        },
        getTeam({commit}) {
            instance.get('/users/team')
                .then((resp) => {
                    commit('teamSuccess', resp.data);
                }).catch((err) => {
                if (err.response.status === 500)
                    commit('teamFailure', err.response.data);
            });
        },

        userLogOut({commit}) {
            commit('authLogout');
            localStorage.removeItem('token');
            localStorage.setItem('user', 'public');
            localStorage.removeItem('username');
        }
    },

    getters: {
        isAuthenticated: state => !!state.token,
        isToken: state => state.token,
        teamId: state => state.userTeam.teamId,
        team: state => state.userTeam,
        team_msg: state => state.team_msg
    },
});

export default store