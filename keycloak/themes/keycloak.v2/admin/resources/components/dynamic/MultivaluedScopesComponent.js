import React, {useState} from "../../_snowpack/pkg/react.js";
import {useTranslation} from "../../_snowpack/pkg/react-i18next.js";
import {Controller, useFormContext} from "../../_snowpack/pkg/react-hook-form.js";
import {
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  TextInput
} from "../../_snowpack/pkg/@patternfly/react-core.js";
import {HelpItem} from "../help-enabler/HelpItem.js";
import {AddScopeDialog} from "../../clients/scopes/AddScopeDialog.js";
import {useAdminClient, useFetch} from "../../context/auth/AdminClient.js";
import {useParams} from "../../_snowpack/pkg/react-router.js";
export const MultivaluedScopesComponent = ({
  defaultValue,
  name
}) => {
  const {t} = useTranslation("dynamic");
  const {control} = useFormContext();
  const {conditionName} = useParams();
  const adminClient = useAdminClient();
  const [open, setOpen] = useState(false);
  const [clientScopes, setClientScopes] = useState([]);
  useFetch(() => adminClient.clientScopes.find(), (clientScopes2) => {
    setClientScopes(clientScopes2);
  }, []);
  const toggleModal = () => {
    setOpen(!open);
  };
  return /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("realm-settings:clientScopesCondition"),
    id: "expected-scopes",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: t("realm-settings-help:clientScopesConditionTooltip"),
      fieldLabelId: "realm-settings:clientScopesCondition"
    }),
    fieldId: name
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: `config.scopes`,
    control,
    defaultValue: [defaultValue],
    rules: {required: true},
    render: ({onChange, value}) => {
      return /* @__PURE__ */ React.createElement(React.Fragment, null, open && /* @__PURE__ */ React.createElement(AddScopeDialog, {
        clientScopes: clientScopes.filter((scope) => !value.includes(scope.name)),
        isClientScopesConditionType: true,
        open,
        toggleDialog: () => setOpen(!open),
        onAdd: (scopes) => {
          onChange([
            ...value,
            ...scopes.map((scope) => scope.scope).map((item) => item.name)
          ]);
        }
      }), value.length === 0 && !conditionName && /* @__PURE__ */ React.createElement(TextInput, {
        type: "text",
        id: "kc-scopes",
        value,
        "data-testid": "client-scope-input",
        name: "config.client-scopes",
        isDisabled: true
      }), /* @__PURE__ */ React.createElement(ChipGroup, {
        className: "kc-client-scopes-chip-group",
        isClosable: true,
        onClick: () => {
          onChange([]);
        }
      }, value.map((currentChip) => /* @__PURE__ */ React.createElement(Chip, {
        key: currentChip,
        onClick: () => {
          onChange(value.filter((item) => item !== currentChip));
        }
      }, currentChip))), /* @__PURE__ */ React.createElement(Button, {
        "data-testid": "select-scope-button",
        variant: "secondary",
        onClick: () => {
          toggleModal();
        }
      }, t("common:select")));
    }
  }));
};
