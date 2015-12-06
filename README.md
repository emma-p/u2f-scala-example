# u2f-scala-example

## Using Yubikey with Linux

If you are using Linux, you might see errors when trying to register the Yubikey.
It may be because your key is trying to use OTP by default instead of U2F (it it uses multiple protocols).

Following [these instructions](https://www.yubico.com/faq/enable-u2f-linux/) (particularly adding the `70-u2f.rules` file as recommended) should solve the problem.

