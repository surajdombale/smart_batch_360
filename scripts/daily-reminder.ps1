# Daily reminder to continue the SmartBatch360 roadmap in small, verified
# chunks (requested by the user 2026-08-25/26) - triggered by a Windows
# Scheduled Task ("SmartBatch360 Daily Reminder", 7:00 PM daily). Shows a
# system-tray balloon notification; doesn't touch the repo or launch
# anything automatically - the user opens Claude Desktop themselves and
# says "continue roadmap" to pick up from docs/06_SCOPE_AND_ROADMAP.md.

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$notifyIcon = New-Object System.Windows.Forms.NotifyIcon
$notifyIcon.Icon = [System.Drawing.SystemIcons]::Information
$notifyIcon.Visible = $true
$notifyIcon.BalloonTipTitle = "SmartBatch360"
$notifyIcon.BalloonTipText = "Time for today's small roadmap chunk. Open Claude Desktop and say 'continue roadmap'."
$notifyIcon.ShowBalloonTip(15000)

Start-Sleep -Seconds 16
$notifyIcon.Dispose()
