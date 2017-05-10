
CREATE DATABASE tictactoe Default character set 'utf8';

use tictactoe;

CREATE TABLE Conn(
ID int primary Key auto_increment,
UName varchar(50) not null,
UPass varchar(50) not null,
UWin int default 0,
ULose int default 0,
UScore int default 0
);

INSERT INTO Conn
(UName,UPass) VALUES
("Kubi","Sarı");

