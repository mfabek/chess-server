CREATE SCHEMA IF NOT EXISTS `chess` DEFAULT CHARACTER SET utf8;

CREATE TABLE IF NOT EXISTS `chess`.`game` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `player_count` INT DEFAULT 0,
    `name` VARCHAR(255),
    `finished` TINYINT(1) DEFAULT FALSE,
    PRIMARY KEY (`id`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `chess`.`board` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `board` VARCHAR(255) NOT NULL,
    `turn` VARCHAR(255),
    `move` VARCHAR(255) NOT NULL,
    `game_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `fk_game_idx` (`game_id` ASC) VISIBLE,
    CONSTRAINT `fk_game`
    FOREIGN KEY (`game_id`)
    REFERENCES `chess`.`game` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
    ENGINE = InnoDB
