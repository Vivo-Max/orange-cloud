const APP_CALLBACK = "orangecloud://oauth/callback";
const CALLBACK_PATH = "/oauth/callback";

export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname !== CALLBACK_PATH) {
      return new Response("Orange Cloud OAuth callback worker", {
        status: url.pathname === "/" ? 200 : 404,
        headers: { "content-type": "text/plain; charset=utf-8" },
      });
    }

    const hasCode = url.searchParams.has("code");
    const hasState = url.searchParams.has("state");
    const hasError = url.searchParams.has("error");

    if (!hasError && (!hasCode || !hasState)) {
      return new Response("Missing OAuth callback parameters", {
        status: 400,
        headers: { "content-type": "text/plain; charset=utf-8" },
      });
    }

    const appCallback = new URL(APP_CALLBACK);
    url.searchParams.forEach((value, key) => {
      appCallback.searchParams.set(key, value);
    });

    return Response.redirect(appCallback.toString(), 302);
  },
};
