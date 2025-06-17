echo "--- Setting up Jenkins worker custom app scripts ---"

rm -rf ${HOME}/allow_permissions_agent
mkdir -p ${HOME}/allow_permissions_agent
cd ${HOME}/allow_permissions_agent

echo "--- Creating osascript for approving allow permissions popup ---"

cat > approve_allow_permissions_popup.scpt <<EOL
-- Auto-click "Allow" on user permission dialogs

delay 2 -- Give time for dialog to appear

log "AutoClickAllow: Started"

set maxRetries to 5

tell application "System Events"
	repeat maxRetries times
		try
			if exists (window 1 of process "UserNotificationCenter") then
				log "AutoClickAllow: Dialog detected."

				tell process "UserNotificationCenter"
					if exists (button "Allow" of window 1) then
						click button "Allow" of window 1
						log "AutoClickAllow: Clicking Allow"
						exit repeat
					else
						log "AutoClickAllow: Allow button not found"
					end if
				end tell
			else
				log "AutoClickAllow: Dialog not found, retrying..."
			end if
		on error errMsg
			log "AutoClickAllow: Error - " & errMsg
		end try
		delay 1
	end repeat
end tell

log "AutoClickAllow: Finished"
EOL

echo "--- Creating launchAgent to run the dismiss system restart popup application at startup ---"

cat > com.jenkins.worker.allow.permissions.plist <<EOL

<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.jenkins.worker.allow.permissions</string>
    <key>UserName</key>
    <string>ec2-user</string>
    <key>SessionCreate</key>
    <true/>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/osascript</string>
        <string>/Users/ec2-user/allow_permissions_agent/approve_allow_permissions_popup.scpt</string>
    </array>
    <key>EnvironmentVariables</key>
    <dict>
      <key>PATH</key>
      <string>/Users/ec2-user/.rbenv/shims:/Users/ec2-user/.rbenv/bin:/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/System/Cryptexes/App/usr/bin:/usr/bin:/bin:/usr/sbin:/sbin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/local/bin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/bin:/var/run/com.apple.security.cryptexd/codex.system/bootstrap/usr/appleinternal/bin:/Library/Apple/usr/bin</string>
    </dict>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>Debug</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/Users/ec2-user/allow_permissions_agent/stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/ec2-user/allow_permissions_agent/error.log</string>
</dict>
</plist>
EOL

cat com.jenkins.worker.allow.permissions.plist

sudo mv com.jenkins.worker.allow.permissions.plist /Library/LaunchAgents/com.jenkins.worker.allow.permissions.plist

sudo chown root:wheel /Library/LaunchAgents/com.jenkins.worker.allow.permissions.plist
sudo chmod 644 /Library/LaunchAgents/com.jenkins.worker.allow.permissions.plist

echo "--- Enabling launchAgent ---"

sudo launchctl bootstrap gui/$(id -u ec2-user) -w /Library/LaunchAgents/com.jenkins.worker.allow.permissions.plist
