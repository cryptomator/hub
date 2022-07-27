import React, {useState} from "../_snowpack/pkg/react.js";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  TextInput
} from "../_snowpack/pkg/@patternfly/react-core.js";
import {useTranslation} from "../_snowpack/pkg/react-i18next.js";
import {Controller, useForm} from "../_snowpack/pkg/react-hook-form.js";
import {useAdminClient} from "../context/auth/AdminClient.js";
import {useAlerts} from "../components/alert/Alerts.js";
import {HelpItem} from "../components/help-enabler/HelpItem.js";
import {useServerInfo} from "../context/server-info/ServerInfoProvider.js";
import {KEY_PROVIDER_TYPE} from "../util.js";
export const JavaKeystoreModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh
}) => {
  const {t} = useTranslation("groups");
  const serverInfo = useServerInfo();
  const adminClient = useAdminClient();
  const {addAlert, addError} = useAlerts();
  const {handleSubmit, control} = useForm({});
  const [isEllipticCurveDropdownOpen, setIsEllipticCurveDropdownOpen] = useState(false);
  const allComponentTypes = serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];
  const save = async (component) => {
    try {
      await adminClient.components.create({
        ...component,
        parentId: component.parentId,
        providerId: providerType,
        providerType: KEY_PROVIDER_TYPE,
        config: {priority: ["0"]}
      });
      handleModalToggle();
      addAlert(t("saveProviderSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("groups:saveProviderError", error);
    }
  };
  return /* @__PURE__ */ React.createElement(Modal, {
    className: "add-provider-modal",
    variant: ModalVariant.medium,
    title: t("addProvider"),
    isOpen: open,
    onClose: handleModalToggle,
    actions: [
      /* @__PURE__ */ React.createElement(Button, {
        "data-testid": "add-provider-button",
        key: "confirm",
        variant: "primary",
        type: "submit",
        form: "add-provider"
      }, t("common:Add")),
      /* @__PURE__ */ React.createElement(Button, {
        id: "modal-cancel",
        "data-testid": "cancel",
        key: "cancel",
        variant: ButtonVariant.link,
        onClick: () => {
          handleModalToggle();
        }
      }, t("common:cancel"))
    ]
  }, /* @__PURE__ */ React.createElement(Form, {
    isHorizontal: true,
    id: "add-provider",
    className: "pf-u-mt-lg",
    onSubmit: handleSubmit(save)
  }, /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("consoleDisplayName"),
    fieldId: "kc-console-display-name",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:displayName",
      fieldLabelId: "realm-settings:consoleDisplayName"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "name",
    control,
    defaultValue: providerType,
    render: ({onChange}) => /* @__PURE__ */ React.createElement(TextInput, {
      "aria-label": t("consoleDisplayName"),
      defaultValue: providerType,
      onChange: (value) => {
        onChange(value);
      },
      "data-testid": "display-name-input"
    })
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("common:enabled"),
    fieldId: "kc-enabled",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: t("realm-settings-help:enabled"),
      fieldLabelId: "enabled"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.enabled",
    control,
    defaultValue: ["true"],
    render: ({onChange, value}) => /* @__PURE__ */ React.createElement(Switch, {
      id: "kc-enabled",
      label: t("common:on"),
      labelOff: t("common:off"),
      isChecked: value[0] === "true",
      "data-testid": value[0] === "true" ? "internationalization-enabled" : "internationalization-disabled",
      onChange: (value2) => {
        onChange([value2 + ""]);
      }
    })
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("active"),
    fieldId: "kc-active",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:active",
      fieldLabelId: "realm-settings:active"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.active",
    control,
    defaultValue: ["true"],
    render: ({onChange, value}) => /* @__PURE__ */ React.createElement(Switch, {
      id: "kc-active",
      label: t("common:on"),
      labelOff: t("common:off"),
      isChecked: value[0] === "true",
      "data-testid": value[0] === "true" ? "internationalization-enabled" : "internationalization-disabled",
      onChange: (value2) => {
        onChange([value2 + ""]);
      }
    })
  })), providerType === "java-keystore" && /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("algorithm"),
    fieldId: "kc-algorithm",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:algorithm",
      fieldLabelId: "realm-settings:algorithm"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.algorithm",
    control,
    defaultValue: ["RS256"],
    render: ({onChange, value}) => /* @__PURE__ */ React.createElement(Select, {
      toggleId: "kc-elliptic",
      onToggle: () => setIsEllipticCurveDropdownOpen(!isEllipticCurveDropdownOpen),
      onSelect: (_, value2) => {
        onChange([value2 + ""]);
        setIsEllipticCurveDropdownOpen(false);
      },
      selections: [value + ""],
      variant: SelectVariant.single,
      "aria-label": t("algorithm"),
      isOpen: isEllipticCurveDropdownOpen,
      placeholderText: "Select one...",
      "data-testid": "select-algorithm"
    }, allComponentTypes[3].properties[3].options.map((p, idx) => /* @__PURE__ */ React.createElement(SelectOption, {
      selected: p === value,
      key: `algorithm-${idx}`,
      value: p
    })))
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("keystore"),
    fieldId: "kc-login-theme",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:keystore",
      fieldLabelId: "realm-settings:keystore"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.keystore",
    control,
    defaultValue: [],
    render: ({onChange}) => /* @__PURE__ */ React.createElement(TextInput, {
      "aria-label": t("keystore"),
      onChange: (value) => {
        onChange([value + ""]);
      },
      "data-testid": "select-display-name"
    })
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("keystorePassword"),
    fieldId: "kc-login-theme",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:keystorePassword",
      fieldLabelId: "realm-settings:keystorePassword"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.keystorePassword",
    control,
    defaultValue: [],
    render: ({onChange}) => /* @__PURE__ */ React.createElement(TextInput, {
      "aria-label": t("consoleDisplayName"),
      onChange: (value) => {
        onChange([value + ""]);
      },
      "data-testid": "select-display-name"
    })
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("keyAlias"),
    fieldId: "kc-login-theme",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:keyAlias",
      fieldLabelId: "realm-settings:keyAlias"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.keyAlias",
    control,
    defaultValue: [],
    render: ({onChange}) => /* @__PURE__ */ React.createElement(TextInput, {
      "aria-label": t("consoleDisplayName"),
      onChange: (value) => {
        onChange([value + ""]);
      },
      "data-testid": "select-display-name"
    })
  })), /* @__PURE__ */ React.createElement(FormGroup, {
    label: t("keyPassword"),
    fieldId: "kc-login-theme",
    labelIcon: /* @__PURE__ */ React.createElement(HelpItem, {
      helpText: "realm-settings-help:keyPassword",
      fieldLabelId: "realm-settings:keyPassword"
    })
  }, /* @__PURE__ */ React.createElement(Controller, {
    name: "config.keyPassword",
    control,
    defaultValue: [],
    render: ({onChange}) => /* @__PURE__ */ React.createElement(TextInput, {
      "aria-label": t("consoleDisplayName"),
      onChange: (value) => {
        onChange([value + ""]);
      },
      "data-testid": "select-display-name"
    })
  })))));
};
