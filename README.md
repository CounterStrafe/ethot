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
`discord-admin-channel-id` (str): Discord channel ID for admin commands.
`discord-announcement-channel-id` (str): Discord channel ID for posting game announcements.
`discord-guild-id` (str): ID of the Discord server.
`discord-server-channel-ids` (vec[str]): Discord channel ID's for each server. Each channel ID gets mapped to the eBot server ID of it's index + 1. For example, the 0th channel ID in this vector gets mapped to the eBot server ID #1.

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
