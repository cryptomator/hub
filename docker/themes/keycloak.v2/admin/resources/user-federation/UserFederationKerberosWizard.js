import {Wizard} from "../_snowpack/pkg/@patternfly/react-core.js";
import {useTranslation} from "../_snowpack/pkg/react-i18next.js";
import React from "../_snowpack/pkg/react.js";
import {KerberosSettingsRequired} from "./kerberos/KerberosSettingsRequired.js";
import {SettingsCache} from "./shared/SettingsCache.js";
import {useForm} from "../_snowpack/pkg/react-hook-form.js";
export const UserFederationKerberosWizard = () => {
  const {t} = useTranslation("user-federation");
  const form = useForm({mode: "onChange"});
  const steps = [
    {
      name: t("requiredSettings"),
      component: /* @__PURE__ */ React.createElement(KerberosSettingsRequired, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      })
    },
    {
      name: t("cacheSettings"),
      component: /* @__PURE__ */ React.createElement(SettingsCache, {
        form,
        showSectionHeading: true,
        showSectionDescription: true
      }),
      nextButtonText: t("common:finish")
    }
  ];
  return /* @__PURE__ */ React.createElement(Wizard, {
    steps
  });
};
