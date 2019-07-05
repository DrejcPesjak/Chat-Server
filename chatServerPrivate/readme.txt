//Author: Drejc Pesjak
//63180224

1.Begining
	First compile server and run it:
		javac ChatServer.java; java ChatServer

	Then compile client and run it:
		javac ChatClient.java; java ChatClient

	The client will be connected to the server and afterwards prompted for username
	Username must be without spaces!

2.Sending messages
	-public
		For public messages just type in your messsage and hit enter.
	-private
		For private messaging use the following format: @username message

3.Reading messages
	Standard public incoming messages use following format: [username time] message
	While private messages start with the letter p.       : p[username time] message

