import React from "../_snowpack/pkg/react.js";
import {useTranslation} from "../_snowpack/pkg/react-i18next.js";
import {ActionGroup, Button, TextInput} from "../_snowpack/pkg/@patternfly/react-core.js";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from "../_snowpack/pkg/@patternfly/react-table.js";
import {MinusCircleIcon, PlusCircleIcon} from "../_snowpack/pkg/@patternfly/react-icons.js";
import {FormAccess} from "../components/form-access/FormAccess.js";
import "./RealmRolesSection.css";
export const RoleAttributes = ({
  form: {register, formState, errors, watch},
  save,
  array: {fields, append, remove},
  reset
}) => {
  const {t} = useTranslation("roles");
  const columns = ["Key", "Value"];
  const watchFirstKey = watch("attributes[0].key", "");
  return /* @__PURE__ */ React.createElement(FormAccess, {
    role: "manage-realm"
  }, /* @__PURE__ */ React.createElement(TableComposable, {
    className: "kc-role-attributes__table",
    "aria-label": "Role attribute keys and values",
    variant: "compact",
    borders: false
  }, /* @__PURE__ */ React.createElement(Thead, null, /* @__PURE__ */ React.createElement(Tr, null, /* @__PURE__ */ React.createElement(Th, {
    id: "key",
    width: 40
  }, columns[0]), /* @__PURE__ */ React.createElement(Th, {
    id: "value",
    width: 40
  }, columns[1]))), /* @__PURE__ */ React.createElement(Tbody, null, fields.map((attribute, rowIndex) => /* @__PURE__ */ React.createElement(Tr, {
    key: attribute.id
  }, /* @__PURE__ */ React.createElement(Td, {
    key: `${attribute.id}-key`,
    id: `text-input-${rowIndex}-key`,
    dataLabel: columns[0]
  }, /* @__PURE__ */ React.createElement(TextInput, {
    name: `attributes[${rowIndex}].key`,
    ref: register(),
    "aria-label": "key-input",
    defaultValue: attribute.key,
    validated: errors.attributes?.[rowIndex] ? "error" : "default"
  })), /* @__PURE__ */ React.createElement(Td, {
    key: `${attribute}-value`,
    id: `text-input-${rowIndex}-value`,
    dataLabel: columns[1]
  }, /* @__PURE__ */ React.createElement(TextInput, {
    name: `attributes[${rowIndex}].value`,
    ref: register(),
    "aria-label": "value-input",
    defaultValue: attribute.value,
    validated: errors.description ? "error" : "default"
  })), rowIndex !== fields.length - 1 && fields.length - 1 !== 0 && /* @__PURE__ */ React.createElement(Td, {
    key: "minus-button",
    id: `kc-minus-button-${rowIndex}`,
    dataLabel: columns[2]
  }, /* @__PURE__ */ React.createElement(Button, {
    id: `minus-button-${rowIndex}`,
    "aria-label": `remove ${attribute.key} with value ${attribute.value} `,
    variant: "link",
    className: "kc-role-attributes__minus-icon",
    onClick: () => remove(rowIndex)
  }, /* @__PURE__ */ React.createElement(MinusCircleIcon, null))), rowIndex === fields.length - 1 && /* @__PURE__ */ React.createElement(Td, {
    key: "add-button",
    id: "add-button",
    dataLabel: columns[2]
  }, fields[rowIndex].key === "" && /* @__PURE__ */ React.createElement(Button, {
    id: `minus-button-${rowIndex}`,
    "aria-label": `remove ${attribute.key} with value ${attribute.value} `,
    variant: "link",
    className: "kc-role-attributes__minus-icon",
    onClick: () => remove(rowIndex)
  }, /* @__PURE__ */ React.createElement(MinusCircleIcon, null)), /* @__PURE__ */ React.createElement(Button, {
    "aria-label": t("roles:addAttributeText"),
    id: "plus-icon",
    variant: "link",
    className: "kc-role-attributes__plus-icon",
    onClick: () => append({key: "", value: ""}),
    icon: /* @__PURE__ */ React.createElement(PlusCircleIcon, null),
    isDisabled: !formState.isValid
  })))))), /* @__PURE__ */ React.createElement(ActionGroup, {
    className: "kc-role-attributes__action-group"
  }, /* @__PURE__ */ React.createElement(Button, {
    "data-testid": "realm-roles-save-button",
    variant: "primary",
    isDisabled: !watchFirstKey,
    onClick: save
  }, t("common:save")), /* @__PURE__ */ React.createElement(Button, {
    onClick: reset,
    variant: "link"
  }, t("common:reload"))));
};
