import React from "../_snowpack/pkg/react.js";
import {useTranslation} from "../_snowpack/pkg/react-i18next.js";
import {
  Button,
  ButtonVariant,
  EmptyState,
  EmptyStateBody,
  Form,
  InputGroup,
  TextInput,
  Title
} from "../_snowpack/pkg/@patternfly/react-core.js";
import {SearchIcon} from "../_snowpack/pkg/@patternfly/react-icons.js";
import {useForm} from "../_snowpack/pkg/react-hook-form.js";
import {useHistory, useRouteMatch} from "../_snowpack/pkg/react-router-dom.js";
export const SearchUser = ({onSearch}) => {
  const {t} = useTranslation("users");
  const {register, handleSubmit} = useForm();
  const {url} = useRouteMatch();
  const history = useHistory();
  const goToCreate = () => history.push(`${url}/add-user`);
  return /* @__PURE__ */ React.createElement(EmptyState, null, /* @__PURE__ */ React.createElement(Title, {
    "data-testid": "search-users-title",
    headingLevel: "h4",
    size: "lg"
  }, t("startBySearchingAUser")), /* @__PURE__ */ React.createElement(EmptyStateBody, null, /* @__PURE__ */ React.createElement(Form, {
    onSubmit: handleSubmit((form) => onSearch(form.search))
  }, /* @__PURE__ */ React.createElement(InputGroup, null, /* @__PURE__ */ React.createElement(TextInput, {
    type: "text",
    id: "kc-user-search",
    name: "search",
    ref: register()
  }), /* @__PURE__ */ React.createElement(Button, {
    variant: ButtonVariant.control,
    "aria-label": t("common:search"),
    type: "submit"
  }, /* @__PURE__ */ React.createElement(SearchIcon, null))))), /* @__PURE__ */ React.createElement(Button, {
    "data-testid": "create-new-user",
    variant: "link",
    onClick: goToCreate
  }, t("createNewUser")));
};
