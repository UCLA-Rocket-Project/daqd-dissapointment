# Data Acquisition Daemon
`daqd` is a data Acquisition daemon for rocket project. It is designed to have a low memory
footprint and support a wide variety of inputs through reflection.

## Usage

Send JSON commands (described below) to the UNIX domain socket (`/tmp/daqd.sock` on *nix systems 
and `C:\Users\<user name>\AppData\Local\Temp\` on Win10 systems by default)

Example command to get the current config:

`echo '{"printConfig": true}' | nc -U /tmp/daqd.sock && echo`

(the second echo is just to get a trailing newline)

## Command Type
In the folder [json](src/main/java/io/github/uclarocketproject/daqd/json)
has all the classes that can be sent as arguments to the domain socket.

Accepted command classes:
* `DaqConfigJson`: change the configuration of the daemon
* `PrintConfigJson`: print out the current configuration
* `ReadJson`: get a snapshot of the last samples made
* `RecordingJson` : set whether the daemon is currently recording to true or false

You can set the `logFile` property of `DaqConfigJson` to `ROTATE` to set the logfile to automatically
rotate each time recording is resumed. The file will be located in your `$HOME` directory with a name
of `<TIMESTAMP>_ROTATE.daqd.log`

## Available Daq Devices
You can specify devices by sending a `DaqConfigJson`. There is a subfield called `className`.
The default package comes with 2 devices [in the deviceTypes folder](src/main/java/io/github/uclarocketproject/daqd/deviceTypes): 
* `SineWaveDevice`: just samples the function `sine(time)`. Use for debugging.
* `ModbusDevice`: samples a modbus serial device