INSERT INTO oauth2_registered_client (
    id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, 
    client_name, client_authentication_methods, authorization_grant_types, 
    redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 
    'crm-client', 
    CURRENT_TIMESTAMP, 
    '{noop}secret', 
    NULL, 
    'CRM Application', 
    'client_secret_basic', 
    'authorization_code,refresh_token', 
    'http://127.0.0.1:3000/crm/callback', 
    'http://127.0.0.1:3000/', 
    'openid,profile', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
);

INSERT INTO oauth2_registered_client (
    id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, 
    client_name, client_authentication_methods, authorization_grant_types, 
    redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'h2h2h2h2-h2h2-h2h2-h2h2-h2h2h2h2h2h2', 
    'hr-client', 
    CURRENT_TIMESTAMP, 
    '{noop}secret', 
    NULL, 
    'HR Application', 
    'client_secret_basic', 
    'authorization_code,refresh_token', 
    'http://127.0.0.1:3000/hr/callback', 
    'http://127.0.0.1:3000/', 
    'openid,profile', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}', 
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",3600.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}'
);
