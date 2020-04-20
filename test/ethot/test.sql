CREATE TABLE `servers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(50) NOT NULL,
  `rcon` varchar(50) NOT NULL,
  `hostname` varchar(100) NOT NULL,
  `tv_ip` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

CREATE TABLE `matchs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(50) DEFAULT NULL,
  `server_id` bigint(20) DEFAULT NULL,
  `season_id` bigint(20) DEFAULT NULL,
  `team_a` bigint(20) DEFAULT NULL,
  `team_a_flag` varchar(2) DEFAULT NULL,
  `team_a_name` varchar(25) DEFAULT NULL,
  `team_b` bigint(20) DEFAULT NULL,
  `team_b_flag` varchar(2) DEFAULT NULL,
  `team_b_name` varchar(25) DEFAULT NULL,
  `status` smallint(6) DEFAULT NULL,
  `is_paused` tinyint(1) DEFAULT NULL,
  `score_a` bigint(20) DEFAULT NULL,
  `score_b` bigint(20) DEFAULT NULL,
  `max_round` mediumint(9) NOT NULL,
  `rules` varchar(200) NOT NULL,
  `overtime_startmoney` bigint(20) DEFAULT NULL,
  `overtime_max_round` mediumint(9) DEFAULT NULL,
  `config_full_score` tinyint(1) DEFAULT NULL,
  `config_ot` tinyint(1) DEFAULT NULL,
  `config_streamer` tinyint(1) DEFAULT NULL,
  `config_knife_round` tinyint(1) DEFAULT NULL,
  `config_switch_auto` tinyint(1) DEFAULT NULL,
  `config_auto_change_password` tinyint(1) DEFAULT NULL,
  `config_password` varchar(50) DEFAULT NULL,
  `config_heatmap` tinyint(1) DEFAULT NULL,
  `config_authkey` varchar(200) DEFAULT NULL,
  `enable` tinyint(1) DEFAULT NULL,
  `map_selection_mode` varchar(255) DEFAULT NULL,
  `ingame_enable` tinyint(1) DEFAULT NULL,
  `current_map` bigint(20) DEFAULT NULL,
  `force_zoom_match` tinyint(1) DEFAULT NULL,
  `identifier_id` varchar(100) DEFAULT NULL,
  `startdate` datetime DEFAULT NULL,
  `auto_start` tinyint(1) DEFAULT NULL,
  `auto_start_time` mediumint(9) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `server_id_idx` (`server_id`),
  KEY `team_a_idx` (`team_a`),
  KEY `team_b_idx` (`team_b`),
  KEY `current_map_idx` (`current_map`),
  KEY `season_id_idx` (`season_id`),
  CONSTRAINT `matchs_server_id_servers_id` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=latin1;

INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (6, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (1, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (2, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (3, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (4, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `servers` (id, ip, rcon, hostname, created_at, updated_at) VALUES (5, 'ip', 'rcon', 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');

INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (1, 1, 1, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (2, 2, 12, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (3, 3, 13, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (4, 4, 13, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (5, 4, 11, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
INSERT INTO `matchs` (id, server_id, `status`, max_round, rules, created_at, updated_at) VALUES (6, 5, 14, 16, 'hostname', '2011-12-18 13:17:17', '2011-12-18 13:17:17');
