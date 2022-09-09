package org.cryptomator.hub.filters;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class VaultAdminOnlyFilterProviderTestConstants {

	static final String VALID_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMTUsImV4cCI6MTUxNjIzOTAzMCwibmJmIjoxNTE2MjM5MDAwfQ.LkN_iQqdmZyvGXAIKYwJZF0zWsscRpfWQr1OEmj-gdqA5yVkn0t1nROSpKk2OErTrqBSf2b5Kap8yPSw3yHBqYLUIpfVCxrfu0IpKM_K_Y31m-XAX5173__5yDOr_k35";
	static final String VALID_TOKEN_VAULT_3000 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDMwMDAifQ.eyJpYXQiOjE1MTYyMzkwMTUsImV4cCI6MTUxNjIzOTAzMCwibmJmIjoxNTE2MjM5MDAwfQ.IYQH9NofEy2A1qYzR4Lg720aJ93AhWXGyc8kF9oOIr7LuFDg-_ZN-0euV34hzcKtWpGkfgPbuazl-7GS1VxDIGcDLG9J6bnQ3hnPpWOYAcxWW-UMqiXJ6tbgryemzhq1";
	static final String EXPIRED_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMDAsImV4cCI6MTUxNjIzOTAxNSwibmJmIjoxNTE2MjM4OTg1fQ.2U342-dfpLV4oN3ZdcKsEpS04xMduuIlViotcczr3_fNy96B4wHOn-I1LibOT_Y6IoFUaoZBiDxzYQSup9S7R2EEUnFGN9bPBBvXwvuT30B8caUebWrvXGTm63kPFCy8";
	static final String FUTURE_ISSUE_AT_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwMzAsImV4cCI6MTUxNjIzOTA0NSwibmJmIjoxNTE2MjM5MDE1fQ.j2FVj1Q3l9vrqgWUluCK_9S_LZnBwuu1KSRJM1_edXnN8jaIhpay7LaUKIPErVUksk49D9ssrWuYDGXZUr45ToGmtjvnbe__pxm8SJbriJbQ-YK3ZQE3im6dNLQYqrUl";
	static final String FUTURE_NOT_BEFORE_TOKEN_VAULT_2 = "eyJhbGciOiJFUzM4NCIsInR5cCI6IkpXVCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.eyJpYXQiOjE1MTYyMzkwNDUsImV4cCI6MTUxNjIzOTA2MCwibmJmIjoxNTE2MjM5MDMwfQ.ptjbTDc-QNgCK9ex5gGCkGhkdXxpoox4vBh7YpIdlrxG52V1q1nS7sYHFthA3ZrTkh5JeuDt7LGMpu26LWwCYd1wesrmjoLTqAfWNkiJVXawCWEf8N76Ms0N1V2OPobE";
	static final String INVALID_SIGNATURE_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCIsInZhdWx0SWQiOiJ2YXVsdDIifQ.e30.cGZDCqzJQgcBHNVPcmBc8JfeGzUf3CHUrwSAMwOA0Dcy9aUZvsAm1dr1MKzuPW_UFHRfMnNi2EwASOA6t-vPWvPFolAHFn5REt2Y9Aw9mIz-qxSBLpz6OMZD16tysQcd";
	static final String MALFORMED_TOKEN = "hello world";

	static final Clock NOW = Clock.fixed(Instant.ofEpochSecond(1516239020), ZoneId.of("UTC"));

}