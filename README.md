# Welcome to Keeper!

Keeper is a password archive that uses double encryption to keep your passwords safe.

Just want to try the app out without downloading and compiling the source? You can download a precompiled JAR of the program [here](https://github.com/this-is-forever/Keeper/releases/tag/1.1). A minimum of JRE 11 is required to run the app.

To compile the program yourself, download the project using git and use Maven to build the program using the following command:

    mvn assembly:assembly
    
You can then run the program by opening the created jar-with-dependencies generated in the target folder.

A minimum of JDK 11 is required to compile the project.

# What is double encryption?

Keeper uses a two-pronged approach to keeping your passwords safe. As you fill your archive with passwords, they are encrypted the moment you stop editing them using a randomized 32-byte key. This key is meant to be kept private and will be saved to the same directory that you keep the Keeper.jar file in.

When you close Keeper, the entirety of your password archive is encrypted with your master password, thus encrypting your passwords twice. Your key file is also encrypted (using a different salt) with your master password, preventing the key from being accessed by anyone without your master password.

Encryption and decryption is done using AES in GCM mode. scrypt is used to generate 64-byte keys from your master password.
