(function(t){function e(e){for(var o,u,s=e[0],i=e[1],l=e[2],p=0,d=[];p<s.length;p++)u=s[p],r[u]&&d.push(r[u][0]),r[u]=0;for(o in i)Object.prototype.hasOwnProperty.call(i,o)&&(t[o]=i[o]);c&&c(e);while(d.length)d.shift()();return a.push.apply(a,l||[]),n()}function n(){for(var t,e=0;e<a.length;e++){for(var n=a[e],o=!0,s=1;s<n.length;s++){var i=n[s];0!==r[i]&&(o=!1)}o&&(a.splice(e--,1),t=u(u.s=n[0]))}return t}var o={},r={app:0},a=[];function u(e){if(o[e])return o[e].exports;var n=o[e]={i:e,l:!1,exports:{}};return t[e].call(n.exports,n,n.exports,u),n.l=!0,n.exports}u.m=t,u.c=o,u.d=function(t,e,n){u.o(t,e)||Object.defineProperty(t,e,{enumerable:!0,get:n})},u.r=function(t){"undefined"!==typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(t,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(t,"__esModule",{value:!0})},u.t=function(t,e){if(1&e&&(t=u(t)),8&e)return t;if(4&e&&"object"===typeof t&&t&&t.__esModule)return t;var n=Object.create(null);if(u.r(n),Object.defineProperty(n,"default",{enumerable:!0,value:t}),2&e&&"string"!=typeof t)for(var o in t)u.d(n,o,function(e){return t[e]}.bind(null,o));return n},u.n=function(t){var e=t&&t.__esModule?function(){return t["default"]}:function(){return t};return u.d(e,"a",e),e},u.o=function(t,e){return Object.prototype.hasOwnProperty.call(t,e)},u.p="/";var s=window["webpackJsonp"]=window["webpackJsonp"]||[],i=s.push.bind(s);s.push=e,s=s.slice();for(var l=0;l<s.length;l++)e(s[l]);var c=i;a.push([0,"chunk-vendors"]),n()})({0:function(t,e,n){t.exports=n("56d7")},"034f":function(t,e,n){"use strict";var o=n("64a9"),r=n.n(o);r.a},"11b6":function(t,e,n){},4825:function(t,e,n){"use strict";var o=n("11b6"),r=n.n(o);r.a},"56d7":function(t,e,n){"use strict";n.r(e);n("cadf"),n("551c"),n("f751"),n("097d");var o=function(){var t=this,e=t.$createElement,n=t._self._c||e;return n("div",{attrs:{id:"app"}},[n("router-view")],1)},r=[],a={name:"app"},u=a,s=(n("034f"),n("2877")),i=Object(s["a"])(u,o,r,!1,null,null,null),l=i.exports,c=n("2b0e"),p=n("795b"),d=n.n(p),f=n("2f62"),m=n("bc3a"),h=n.n(m),g="http://localhost:8088",b=h.a.create({baseURL:g,timeout:1e3,headers:{common:{Authorization:"Bearer "+localStorage.getItem("token")},post:{"Content-Type":"application/json"}}});c["default"].use(f["a"]);var v=new f["a"].Store({state:{token:localStorage.getItem("token")||"",status:"",loader:!1},mutations:{authSuccess:function(t,e){t.token=e,t.status="success"},LOADER:function(t,e){t.loader=e},authError:function(t){t.token="",t.status="error"},authLogout:function(t){t.token=""}},actions:{login:function(t,e){var n=new URLSearchParams;return n.append("login",e.login),n.append("password",e.password),console.log(e),new d.a(function(e,o){b.post("/login",n).then(function(n){var o=n.data.token;console.log(o),t.commit("authSuccess",o),localStorage.setItem("token",o),b.defaults.headers.common["Authorization"]="Bearer "+o,e(n)}).catch(function(e){localStorage.removeItem("token"),t.commit("authError"),console.log(e),console.log("no profit =("),o(e)})})}},getters:{isAuthenticated:function(t){return!!t.token},authStatus:function(t){return t.status},menus:function(t,e){return e.isAuthenticated?[{name:"Logout",route:"Logout"}]:[{name:"Login",route:"Login"}]}}}),w=(n("7f7f"),n("6762"),n("8c4f")),y=function(){var t=this,e=t.$createElement,n=t._self._c||e;return n("div",{attrs:{id:"login-form"}},[n("b-form",{on:{submit:t.check}},[n("b-form-group",{attrs:{id:"exampleInputGroup1",label:"Username:","label-for":"exampleInput1",description:"Enter your username"}},[n("b-form-input",{attrs:{id:"exampleInput1",type:"text",required:"",placeholder:"Enter email"},model:{value:t.login,callback:function(e){t.login=e},expression:"login"}})],1),n("b-form-group",{attrs:{id:"exampleInputGroup2",label:"Your password:","label-for":"exampleInput2"}},[n("b-form-input",{attrs:{id:"exampleInput2",type:"password",required:"",placeholder:"Enter password"},model:{value:t.password,callback:function(e){t.password=e},expression:"password"}})],1),n("b-button",{attrs:{type:"submit",variant:"primary"}},[t._v("Log in")])],1),n("transition",[t.success?t._e():n("b-modal",{attrs:{id:"modal1",title:"BootstrapVue"}},[n("p",{staticClass:"my-4"},[t._v("Wrong login or password!")])])],1)],1)},k=[],x={name:"Login",data:function(){return{login:"",password:""}},methods:{check:function(){var t=this;this.$store.dispatch("login",{login:this.login,password:this.password}).then(function(){t.$router.push("/secured")}),this.login="",this.password=""}}},S=x,_=(n("4825"),Object(s["a"])(S,y,k,!1,null,"096d18e7",null)),O=_.exports,j=function(){var t=this,e=t.$createElement,n=t._self._c||e;return n("v-calendar",{attrs:{"is-double-paned":"",attributes:t.attributes}})},I=[],L={data:function(){return{attributes:[{highlight:{backgroundColor:"#ff8080",borderColor:"#ff6666",borderWidth:"2px",borderStyle:"solid"},contentStyle:{color:"white"},dates:[{start:new Date(2019,2,19),end:new Date(2019,2,25)}]}]}}},E=L,P=Object(s["a"])(E,j,I,!1,null,"78fe7ae6",null),A=P.exports;c["default"].use(w["a"]);var $=new w["a"]({routes:[{path:"/",name:"Login",component:O},{path:"/calendar",name:"calendar",component:A}]}),C=["Login","Calendar"];$.beforeEach(function(t,e,n){C.includes(t.name)?n():v.getters.isAuthenticated?n():n("/")});var B=$,D=n("da28"),M=n.n(D),T=(n("4418"),n("ce5b")),R=n.n(T),U=(n("bf40"),n("9f7b")),W=n.n(U);n("f9e3"),n("2dd8");window.Bus=new c["default"],window.token=localStorage.getItem("token"),window.user=localStorage.getItem("user"),c["default"].use(M.a,{firstDayOfWeek:2}),c["default"].use(R.a),c["default"].use(W.a),c["default"].config.productionTip=!1,new c["default"]({render:function(t){return t(l)},router:B,store:v,instance:b}).$mount("#app")},"64a9":function(t,e,n){}});
//# sourceMappingURL=app.af54781b.js.map