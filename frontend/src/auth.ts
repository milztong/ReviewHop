import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

const publicIssuer = process.env.AUTH_KEYCLOAK_ISSUER
  ?? "http://localhost:8180/realms/ankileet";
const internalIssuer = process.env.AUTH_KEYCLOAK_INTERNAL_ISSUER ?? publicIssuer;

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.AUTH_KEYCLOAK_ID ?? "ankileet-web",
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET ?? "",
      issuer: publicIssuer,
      authorization: `${publicIssuer}/protocol/openid-connect/auth`,
      token: `${internalIssuer}/protocol/openid-connect/token`,
      userinfo: `${internalIssuer}/protocol/openid-connect/userinfo`,
      jwks_endpoint: `${internalIssuer}/protocol/openid-connect/certs`,
    }),
  ],
  session: {
    strategy: "jwt",
    maxAge: 60 * 60,
  },
  callbacks: {
    jwt({ token, account }) {
      if (account?.access_token) {
        token.backendAccessToken = account.access_token;
        token.backendAccessTokenExpiresAt = account.expires_at;
      }
      return token;
    },
  },
});
