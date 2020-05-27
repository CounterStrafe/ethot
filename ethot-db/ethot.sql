SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


CREATE DATABASE IF NOT EXISTS ethot;
USE ethot;

CREATE TABLE IF NOT EXISTS `veto` (
  `match_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `tournament_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `ebot_match_id` int(255) NOT NULL,
  `team1_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `team2_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `de_inferno` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 0,
  `de_overpass` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 0,
  `de_shortnuke` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 0,
  `de_train` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 0,
  `de_vertigo` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 0,
  `discord_channel_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `next_ban_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `active` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT 1,
  PRIMARY KEY (`match_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `delays` (
  `match_id` varchar(200) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`match_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `reports` (
  `ebot_match_id` int(255) NOT NULL,
  `report_status` int(255) NOT NULL,
  PRIMARY KEY (`ebot_match_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

GRANT ALL PRIVILEGES ON ethot.* TO 'ebotv3';

COMMIT;
