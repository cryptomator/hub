import {
  Button,
  Wizard,
  WizardContextConsumer,
  WizardFooter
} from "../_snowpack/pkg/@patternfly/react-core.js";
import React from "../_snowpack/pkg/react.js";
import {LdapSettingsGeneral} from "./ldap/LdapSettingsGeneral.js";
import {LdapSettingsConnection} from "./ldap/LdapSettingsConnection.js";
import {LdapSettingsSearching} from "./ldap/LdapSettingsSearching.js";
import {LdapSettingsSynchronization} from "./ldap/LdapSettingsSynchronization.js";
import {LdapSettingsKerberosIntegration} from "./ldap/LdapSettingsKerberosIntegration.js";
import {SettingsCache} from "./shared/SettingsCache.js";
import {LdapSettingsAdvanced} from "./ldap/LdapSettingsAdvanced.js";
import {useTranslation} from "../_snowpack/pkg/react-i18next.js";
import {useForm} from "../_snowpack/pkg/react-hook-form.js";
export const UserFederationLdapWizard = () => {
  const form = useForm();
  const {t} = useTranslation("user-federation");
  const steps = [
    {
      name: t("requiredSettings"),
      id: "ldapRequiredSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsGeneral, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("connectionAndAuthenticationSettings"),
      id: "ldapConnectionSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsConnection, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("ldapSearchingAndUpdatingSettings"),
      id: "ldapSearchingSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsSearching, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("synchronizationSettings"),
      id: "ldapSynchronizationSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsSynchronization, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("kerberosIntegration"),
      id: "ldapKerberosIntegrationSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsKerberosIntegration, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("cacheSettings"),
      id: "ldapCacheSettingsStep",
      component: /* @__PURE__ */ React.createElement(SettingsCache, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("advancedSettings"),
      id: "ldapAdvancedSettingsStep",
      component: /* @__PURE__ */ React.createElement(LdapSettingsAdvanced, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    }
  ];
  const footer = /* @__PURE__ */ React.createElement(WizardFooter, null, /* @__PURE__ */ React.createElement(WizardContextConsumer, null, ({activeStep, onNext, onBack, onClose}) => {
    if (activeStep.id === "ldapRequiredSettingsStep") {
      return /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(Button, {
        variant: "primary",
        type: "submit",
        onClick: onNext
      }, t("common:next")), /* @__PURE__ */ React.createElement(Button, {
        variant: "secondary",
        onClick: onBack,
        className: "pf-m-disabled"
      }, t("common:back")), /* @__PURE__ */ React.createElement(Button, {
        variant: "link",
        onClick: onClose
      }, t("common:cancel")));
    } else if (activeStep.id === "ldapConnectionSettingsStep" || activeStep.id === "ldapSearchingSettingsStep") {
      return /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(Button, {
        variant: "primary",
        type: "submit",
        onClick: onNext
      }, t("common:next")), /* @__PURE__ */ React.createElement(Button, {
        variant: "secondary",
        onClick: onBack
      }, t("common:back")), /* @__PURE__ */ React.createElement(Button, {
        variant: "link",
        onClick: onClose
      }, t("common:cancel")));
    } else if (activeStep.id === "ldapAdvancedSettingsStep") {
      return /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(Button, null, t("common:finish")), /* @__PURE__ */ React.createElement(Button, {
        variant: "secondary",
        onClick: onBack
      }, t("common:back")), /* @__PURE__ */ React.createElement(Button, {
        variant: "link",
        onClick: onClose
      }, t("common:cancel")));
    }
    return /* @__PURE__ */ React.createElement(React.Fragment, null, /* @__PURE__ */ React.createElement(Button, {
      onClick: onNext
    }, "Next"), /* @__PURE__ */ React.createElement(Button, {
      variant: "secondary",
      onClick: onBack
    }, "Back"), /* @__PURE__ */ React.createElement(Button, {
      variant: "link"
    }, t("common:skipCustomizationAndFinish")), /* @__PURE__ */ React.createElement(Button, {
      variant: "link",
      onClick: onClose
    }, t("common:cancel")));
  }));
  return /* @__PURE__ */ React.createElement(Wizard, {
    height: "100%",
    steps,
    footer
  });
};
