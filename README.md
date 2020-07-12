# ethot

A Discord bot to fill the void between Toornament and eBot

## Usage

### Add Your Secrets
Copy `secrets-copy.edn` to `secrets.edn` in the project root and add your
Discord, eBot, and Toornament credentials:
```clojure
{
  ...
  :discord-token "<insert-discord-bot-token-here>"
  ...
}
```

### Run
```
docker-compose up -d
```

## Settings
All settings are managed in the `secrets.edn` file.
```clojure
{
  ; Discord channel ID for admin commands.
  :discord-admin-channel-id ""
  ; Discord channel ID for posting game announcements.
  :discord-announcement-channel-id ""
  ; ID of the Discord server.
  :discord-guild-id ""
  ; Discord channel ID's for each server. Each channel ID gets mapped to the eBot server ID of it's index + 1. For example, the 0th channel ID in this vector gets mapped to the eBot server ID #1.
  :discord-server-channel-ids [""]
  :discord-test-user-ids [""]
  :discord-token ""
  :ebot-admin-user ""
  :ebot-admin-pass ""
  :ebot-base-url ""
  :game-server-password
  :import-blacklist #{""}
  :map-pool ["de_inferno" "de_overpass" "de_train" "de_shortnuke" "de_vertigo"]
  :server-id-range [1 17]
  :toornament-api-key ""
  :toornament-client-id ""
  :toornament-client-secret ""
}
```

## Commands
### Admin Commands
- **!run-stage** *tournament-id stage-name*

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
