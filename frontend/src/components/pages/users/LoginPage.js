import React from 'react';
//import reactMixin from 'react-mixin';
//import ReactFireMixin from 'reactfire';
import TextField from 'material-ui/lib/text-field';
import RaisedButton from 'material-ui/lib/raised-button';
import { red500 } from 'material-ui/lib/styles/colors';
import CircularProgress from 'material-ui/lib/circular-progress';

const isNullOrWhitespace = ( str ) => {
    if (typeof str === 'undefined' || str == null)
      return true;

    return str.replace(/\s/g, '').length < 1;
}

class LoginPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      email: '',
      emailerror: '',
      password: '',
      passworderror: '',
      wholeerror: '',
      state: this.props.user == null ? 'unauthorized' : 'authorized'
    };
  }
  onEmailChange(e) {
    this.setState({
      email: e.target.value,
      emailerror: ''
    });
  }
  onPasswordChange(e) {
    this.setState({
      password: e.target.value,
      passworderror: ''
    });
  }
  onLogin() {
    const { firebaseStore, user } = this.props;
    if (user != null)
      return;
    var errors = false;
    if (isNullOrWhitespace(this.state.email)) {
      this.setState({
        emailerror: 'Обязательное поле'
      });
      errors = true;
    }
    if (isNullOrWhitespace(this.state.password)) {
      this.setState({
        passworderror: 'Обязательное поле'
      });
      errors = true;
    }
    if (!errors)
    {
      this.setState({
        state: 'loading',
        wholeerror: ''
      });

      firebaseStore.loginWithPW({
        'email': this.state.email,
        'password': this.state.password
      }, this.loginCb.bind(this));
    }
  }
  onLogout() {
    //console.log(123);
    const { firebaseStore, user } = this.props;
    if (user == null)
      return;

    firebaseStore.logout(this.logoutCb.bind(this));
  }
  logoutCb(error, user) {
      if (error) {
        this.setState({
          wholeerror: error
        });
      } else {
        this.setState({
          state: 'unauthorized',
          email: '',
          emailerror: '',
          password: '',
          passworderror: '',
          wholeerror: ''
        });
      }
  }
  loginCb(error, user) {
      if (error) {
        this.setState({
          state: 'unauthorized',
          wholeerror: error
        });
      } else {
        this.setState({
          state: 'authorized',
          wholeerror: ''
        });
      }
  }
  render_unauthorized() {
    return (
      <div>
        <span style={{color: red500}}>{this.state.wholeerror}</span>
        <br/>
        <TextField
          floatingLabelText='email'
          value={this.state.email}
          errorText={this.state.emailerror}
          onChange={this.onEmailChange.bind(this)}
        />
        <br/>
        <TextField
          floatingLabelText='Пароль'
          type='password'
          value={this.state.password}
          errorText={this.state.passworderror}
          onChange={this.onPasswordChange.bind(this)}
        />
        <br/>
        <RaisedButton label='Войти' primary={true} onMouseUp={this.onLogin.bind(this)} />
      </div>
    );
  }
  render_authorized() {
    const { user } = this.props;
    return (
      <div>
        <span style={{color: red500}}>{this.state.wholeerror}</span>
        <br/>
        Вы вошли как {user != null ? user.fullname : <CircularProgress size={0.5} />}
        <br/>
        <RaisedButton label='Выйти' primary={true} onMouseUp={this.onLogout.bind(this)}/>
      </div>
    );
  }
  render_loading() {
    return (
      <div>
        <span style={{color: red500}}>{this.state.wholeerror}</span>
        <br/>
        <CircularProgress />
      </div>
    );
  }
  render() {
    const { state } = this.state;
    if (state == 'unauthorized')
      return this.render_unauthorized();
    else if (state == 'authorized')
      return this.render_authorized();
    else if (state == 'loading')
      return this.render_authorized();
    else
      return (<div>критическая ошибка</div>);
  }
}
//LoginPage.propTypes = { initialCount: React.PropTypes.number };
//LoginPage.defaultProps = { initialCount: 0 };
//reactMixin(LoginPage.prototype, ReactFireMixin);

export default LoginPage;
