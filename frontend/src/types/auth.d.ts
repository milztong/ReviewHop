import "@auth/core/jwt";

declare module "@auth/core/jwt" {
  interface JWT {
    backendAccessToken?: string;
    backendAccessTokenExpiresAt?: number;
  }
}
