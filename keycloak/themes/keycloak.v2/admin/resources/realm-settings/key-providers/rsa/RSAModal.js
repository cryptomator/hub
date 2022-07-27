import React from "../../../_snowpack/pkg/react.js";
import {Modal, ModalVariant} from "../../../_snowpack/pkg/@patternfly/react-core.js";
import {useTranslation} from "../../../_snowpack/pkg/react-i18next.js";
import {RSAForm} from "./RSAForm.js";
export const RSAModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh
}) => {
  const {t} = useTranslation("realm-settings");
  return /* @__PURE__ */ React.createElement(Modal, {
    className: "add-provider-modal",
    variant: ModalVariant.medium,
    title: t("addProvider"),
    isOpen: open,
    onClose: handleModalToggle
  }, /* @__PURE__ */ React.createElement(RSAForm, {
    providerType,
    handleModalToggle,
    refresh
  }));
};
