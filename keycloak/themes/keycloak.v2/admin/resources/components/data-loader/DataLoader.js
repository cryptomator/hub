import React, {useState} from "../../_snowpack/pkg/react.js";
import {useFetch} from "../../context/auth/AdminClient.js";
import {KeycloakSpinner} from "../keycloak-spinner/KeycloakSpinner.js";
export function DataLoader(props) {
  const [data, setData] = useState();
  useFetch(() => props.loader(), (result) => setData(result), props.deps || []);
  if (data) {
    if (props.children instanceof Function) {
      return props.children(data);
    }
    return props.children;
  }
  return /* @__PURE__ */ React.createElement(KeycloakSpinner, null);
}
