import React from 'react';
import { Router, Route, IndexRedirect, IndexRoute } from 'react-router';

import UserManager from 'components/pages/users/UserManager';
import Terms from 'components/pages/journal/Terms';
import Courses from 'components/pages/journal/Courses';
import Journal from 'components/pages/journal/Journal';
import StudentGroups from 'components/pages/journal/StudentGroups';
import Categories from 'components/pages/files/Categories';
import Files from 'components/pages/files/Files';
import LayoutContainer from 'components/LayoutContainer';

//StudentGroups

const Routes = (
    <Route path="/" component={LayoutContainer}>
      <IndexRedirect to="/journal" />
      <Route path="/journal">
        <IndexRoute component={Terms} />
        <Route path=":academicTermUid">
          <IndexRoute component={Courses} />
          <Route path=":courseUid">
            <IndexRoute component={StudentGroups} />
            <Route path=":studentGroupUid">
              <IndexRoute component={Journal} />
            </Route>
          </Route>
        </Route>
      </Route>
      <Route path="/files" >
        <IndexRoute component={Categories} />
        <Route path=":categoryUid" >
          <IndexRoute component={Files} />
        </Route>
      </Route>
      <Route
        path="/users"
        components={UserManager} />
    </Route>
);

export default Routes;
