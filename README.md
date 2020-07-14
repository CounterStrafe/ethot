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
  ; Discord channel ID for admin commands
  :discord-admin-channel-id ""
  ; Discord channel ID for posting game announcements
  :discord-announcement-channel-id ""
  ; ID of the Discord server
  :discord-guild-id ""
  ; Discord channel ID's for each server. Each channel ID gets mapped to the eBot server ID of it's index + 1.
  ; For example, the 0th channel ID in this vector gets mapped to the eBot server ID #1.
  :discord-server-channel-ids [""]
  ; Discord bot token
  :discord-token ""
  ; eBot admin username
  :ebot-admin-user ""
  ; eBot admin password
  :ebot-admin-pass ""
  ; eBot base URL
  :ebot-base-url ""
  ; CSGO server password to use for all servers
  :game-server-password
  ; Toornament match ID's not to be imported
  :import-blacklist #{""}
  ; CSGO map pool
  :map-pool ["de_inferno" "de_overpass" "de_train" "de_shortnuke" "de_vertigo"]
  ; eBot server ID range. ethot will user available servers in ascending numerical order.
  ; Currrently the number of available servers needs to be equal to or greater than the number of games that can be played at once.
  :server-id-range [1 17]
  ; Toornament API key
  :toornament-api-key ""
  ; Toornament client ID
  :toornament-client-id ""
  ; Toornament client secret
  :toornament-client-secret ""
}
```

## Commands
### Admin Commands
- **!run-stage** *tournament-id stage-name* Starts the stage. Only one stage can be run at time.
- **!stop-stage** Stops the stage.
- **!delay** *match-id* Delays the match until manually resumed.
- **!resume** *match-id* Resumes the match.

### Player Commands
- **!report** Reports a cheater in the current match. Tise notifies the admins and delays the match export until manually resumed.
- **!ban** *map-name* Bans a map in the veto lobby.

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
