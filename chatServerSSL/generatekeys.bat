@echo off

echo Generating public private key pair NUM1
keytool -genkey -alias jnprivate -keystore jn.private -storetype JKS -keyalg rsa -dname "CN=Janez Novak" -storepass jnpwd1 -keypass jnpwd1 -validity 365

echo Generating public private key pair NUM2
keytool -genkey -alias mnprivate -keystore mn.private -storetype JKS -keyalg rsa -dname "CN=Marija Novak" -storepass mnpwd1 -keypass mnpwd1 -validity 365

echo Generating public private key pair NUM3
keytool -genkey -alias fhprivate -keystore fh.private -storetype JKS -keyalg rsa -dname "CN=Franc Horvat" -storepass fhpwd1 -keypass fhpwd1 -validity 365

echo Generating server public private key pair
keytool -genkey -alias serverprivate -keystore server.private -storetype JKS -keyalg rsa -dname "CN=localhost" -storepass serverpwd -keypass serverpwd -validity 365

echo Generating client public key file
keytool -export -alias jnprivate -keystore jn.private -file temp.key -storepass jnpwd1
keytool -import -noprompt -alias jnpublic -keystore client.public -file temp.key -storepass public
rm temp.key
keytool -export -alias mnprivate -keystore mn.private -file temp.key -storepass mnpwd1
keytool -import -noprompt -alias mnpublic -keystore client.public -file temp.key -storepass public
rm temp.key
keytool -export -alias fhprivate -keystore fh.private -file temp.key -storepass fhpwd1
keytool -import -noprompt -alias fhpublic -keystore client.public -file temp.key -storepass public
rm temp.key


echo Generating server public key file
keytool -export -alias serverprivate -keystore server.private -file temp.key -storepass serverpwd
keytool -import -noprompt -alias serverpublic -keystore server.public -file temp.key -storepass public
rm temp.key
