import { setUser, setUserLoad, cleanUser } from 'actions/userActions';
import { toggleLeftNav, toggleLoginDialog, toggleProfileDialog} from 'actions/layoutActions';
import { isNullOrWhitespace } from 'utils/Utils';

export const CHANGE_EMAIL = 'CHANGE_EMAIL';
export const CHANGE_PASSWORD = 'CHANGE_PASSWORD';
export const SET_LOGIN_ERROR = 'SET_LOGIN_ERROR';
export const SET_EMAIL_ERROR = 'SET_EMAIL_ERROR';
export const SET_PASSWORD_ERROR = 'SET_PASSWORD_ERROR';
export const CLEAN_LOGIN = 'CLEAN_LOGIN';
export const LOGIN = 'LOGIN';
export const LOGOUT = 'LOGIN';

export function changeEmail(email) {
  return {
    type: CHANGE_EMAIL,
    playload: email
  };
}
export function changePassword(password) {
  return {
    type: CHANGE_PASSWORD,
    playload: password
  };
}
export function setEmailError(error) {
  return {
    type: SET_EMAIL_ERROR,
    playload: error
  };
}
export function setPasswordError(error) {
  return {
    type: SET_PASSWORD_ERROR,
    playload: error
  };
}
export function setLoginError(error) {
  return {
    type: SET_LOGIN_ERROR,
    playload: error
  };
}
export function cleanLogin() {
  return {
    type: CLEAN_LOGIN
  };
}
export function initAuth() {
  return (dispatch, getState) => {
    console.log(`initAuth`);
    const { firebaseService } = getState();
    const authData = firebaseService.ref.getAuth();

    if (authData == null)
      return;

    const nowtimestamp = Date.now();
    const timeforsessionleft = authData.expires*1000 - nowtimestamp - 2000;

    console.log(`authData.expires*1000=${authData.expires*1000}, Date.now()=${nowtimestamp}, timeforsessionleft=${timeforsessionleft}`);
    if (timeforsessionleft <= 0)
      return;

    dispatch(setUserLoad());
    firebaseService.getUserFromFirebase(authData.uid, function (snap) {
      //console.log('must be set user');
      dispatch(setUser(snap.val()));
    });
  }
}
export function login(externaldata) {
  return (dispatch, getState) => {
    const { firebaseService, user, loginPanel} = getState();

    const email = externaldata != void 0 ? externaldata.email : loginPanel.email;
    const password = externaldata != void 0 ? externaldata.password : loginPanel.password;
    //console.log(externaldata, email, password);
    if (user.data != null)
      return;

    var errors = false;
    if (isNullOrWhitespace(email)) {
      dispatch(setEmailError('Обязательное поле'));
      errors = true;
    }
    if (isNullOrWhitespace(password)) {
      dispatch(setPasswordError('Обязательное поле'));
      errors = true;
    }
    if (!errors)
    {
      dispatch([
        setUserLoad(),
        setLoginError('')
      ]);

      firebaseService.loginWithPW({
        'email': email,
        'password': password
      }, loginCb.bind(null, dispatch, getState));
    }
  }
}
export function logout() {
  return (dispatch, getState) => {
    const { firebaseService, user } = getState();
    if (user.data == null)
      return;

    firebaseService.logout(logoutCb.bind(null, dispatch, getState));
  }
}

const logoutCb = (dispatch, getState, error, user) => {
  dispatch([
    cleanUser(),
    toggleProfileDialog()
  ]);
}
const loginCb = (dispatch, getState, error, user) => {
  if (error) {
    dispatch([
      cleanUser(),
      setLoginError(error)
    ]);
  } else {
    dispatch([
      setUser(user),
      cleanLogin(),
      toggleLoginDialog()
    ]);
    ;
  }
}
