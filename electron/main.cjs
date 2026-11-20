const { app, BrowserWindow, shell } = require('electron')
const path = require('node:path')

function createWindow() {
  const win = new BrowserWindow({
    width: 430,
    height: 900,
    minWidth: 360,
    minHeight: 640,
    title: '平安托付',
    backgroundColor: '#f7f8fa',
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  })

  win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))

  // 外部链接交给系统浏览器
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

app.whenReady().then(() => {
  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
