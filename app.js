const {app, BrowserWindow} = require('electron');
const path = require('path');
const url = require('url');
const {Detector, Models} = require('snowboy');

// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let win;
let models = new Models();

function startMicListening() {
    models.add({file: 'resources/alexa.pmdl', sensitivity: '0.5', hotwords: 'alexa'});

    let detector = new Detector({
        resource: "resources/common.res",
        models: models,
        audioGain: 2.0
    });

    detector.on('hotword', function (index, hotword) {
        console.log("Hi," + hotword);
        // if(hotword === 'alexa') {
//         let file = fs.createWriteStream('command.wav', { encoding: 'binary' })
//         record.start({
//           sampleRateHertz: 16000,  // this stops recording immediately
//     	  threshold: 0,
//           verbose: true
//         }).pipe(file);
//
//     	setTimeout(function() {
//     		record.stop()
//     	}, 5000)
//       }
    });

    record.start({
        sampleRateHertz: 16000,
        threshold: 0,
    }).pipe(detector);

    console.log('Listening...');
}

function createWindow() {
    // Create the browser window.
    win = new BrowserWindow({width: 800, height: 600});

    // and load the index.html of the app.
    win.loadURL(url.format({
        pathname: path.join(__dirname, 'index.html'),
        protocol: 'file:',
        slashes: true
    }));

    // Emitted when the window is closed.
    win.on('closed', () => {
        // Dereference the window object, usually you would store windows
        // in an array if your app supports multi windows, this is the time
        // when you should delete the corresponding element.
        win = null
    })
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow);

// Quit when all windows are closed.
app.on('window-all-closed', () => {
    // On macOS it is common for applications and their menu bar
    // to stay active until the user quits explicitly with Cmd + Q
    if (process.platform !== 'darwin') {
        app.quit()
    }
});

app.on('activate', () => {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (win === null) {
        createWindow()
    }
});

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.