import React from 'react';
import reactMixin from 'react-mixin';
import ReactFireMixin from 'reactfire';
import Highlighter from 'components/Highlighter';
import List from 'material-ui/lib/lists/list';
import ListItem from 'material-ui/lib/lists/list-item';
import Divider from 'material-ui/lib/divider';
import Subheader from 'material-ui/lib/Subheader';
import Avatar from 'material-ui/lib/avatar';
import {grey400, darkBlack, lightBlack, pinkA200} from 'material-ui/lib/styles/colors';
import IconButton from 'material-ui/lib/icon-button';
import MoreVertIcon from 'material-ui/lib/svg-icons/navigation/more-vert';
import AddCircleIcon from 'material-ui/lib/svg-icons/content/add-circle';
import IconMenu from 'material-ui/lib/menus/icon-menu';
import MenuItem from 'material-ui/lib/menus/menu-item';
import ActionGrade from 'material-ui/lib/svg-icons/action/grade';
import CircularProgress from 'material-ui/lib/circular-progress';
import RaisedButton from 'material-ui/lib/raised-button';
import FlatButton from 'material-ui/lib/flat-button';
import Dialog from 'material-ui/lib/dialog';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { push, replace, go, goForward, goBack } from 'react-router-redux';
import TextField from 'material-ui/lib/text-field';
import Breadcrumbs from 'components/Breadcrumbs';
import { Link } from 'react-router';
import ToggleDisplay from 'react-toggle-display';
import shallowequal from 'shallowequal';
import { Actions } from 'actions/filesActions';
import Checkbox from 'material-ui/lib/checkbox';

class CategoriesDialog extends React.Component {
  constructor(props) {
    super(props);
  }
  componentWillMount() {

  }
  shouldComponentUpdate(nextProps, nextState) {
    return !(this.props.state === 'hide' && nextProps.state === 'hide')
      || !shallowequal(this.props, nextProps);
  }
  componentWillUnmount() {
    //this.unbind('items');
  }

  onSave() {
    const {
      itemKey,
      name,
      allowedForTeachers
    } = this.props;
    this.props.actions.saveCategoryFromDialog({
      itemKey,
      name,
      allowedForTeachers
    });

    this.props.actions.setCategoryDialogState(null);
  }

  render() {
    const {
      itemKey,
      state,
      name,
      allowedForTeachers
    } = this.props;
    var actions = [
      <FlatButton
        label="Отмена"
        primary={true}
        onTouchTap={() => this.props.actions.setCategoryDialogState(null)}
      />
    ];
    if (state === 'create') {
      actions = [
        ...actions,
        <FlatButton
          label="Создать"
          primary={true}
          onTouchTap={this.onSave.bind(this)}
          disabled={name === ''}
        />
      ]
    }
    if (state === 'edit') {
      actions = [
        ...actions,
        <FlatButton
          label="Сохранить"
          primary={true}
          onTouchTap={this.onSave.bind(this)}
          disabled={name === ''}
        />
      ]
    }
    //console.log(this.props);
    return <Dialog
              title={state === 'create' ? 'Создать' : 'Редактировать'}
              actions={actions}
              modal={false}
              open={state !== 'hide'}
              onRequestClose={() => this.props.actions.setCategoryDialogState(null)}
            >
        <TextField
          floatingLabelText="Название"
          value={name}
          onChange={(e) => this.props.actions.setCategoryDialogState({name: e.target.value})} />
        <br />
          <Checkbox
            label="Доступна преподавателям"
            checked={allowedForTeachers}
            onCheck={(e, checked) => this.props.actions.setCategoryDialogState({allowedForTeachers: checked})}
          />
      </Dialog>
  }
}
reactMixin(CategoriesDialog.prototype, ReactFireMixin);

function mapStateToProps(state, ownProps) {
  return {
    ...ownProps,
    // redux store
    ...state.files.categoryDialog,
    user: state.user
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(Actions, dispatch)
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(CategoriesDialog);
