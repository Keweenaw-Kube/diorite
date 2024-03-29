# Diorite

Diorite is an ultra-simple whitelist plugin for Minecraft. When a player joins, it makes a GET request to an external HTTP API containing the player UUID (not containing dashes) like so:

```
GET https://example.com?uuid=player-id
```

If the API responds with 401, the player is blocked from joining and the body of the HTTP response is sent to the player as a reason. Otherwise, the player is allowed to join.

### Usage

Download a build from the [Releases page](https://github.com/Keweenaw-Kube/diorite/releases) and put it in the `mods/` directory.

After running the server with it installed for the first time, a config file will be created at `mods/diorite/config.yaml`. Fill in `endpoint` with the URL you want Diorite to call. There's a few additional, optional properties:

```yml
# Will make request https://example.com?uuid=player-id&token=my-auth-token
endpoint: "https://example.com"
queryParams:
  token: "my-auth-token"
# If true, will make call on player disconnect like https://example.com?uuid=player-id&state=disconnected
callOnDisconnect: false
```

### Development

To publish a new version on GitHub, create a new Git tag with the desired version number: `git tag v1.2.3`. Push the tag, and the GitHub workflow will automatically build and create a new release.

### License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
