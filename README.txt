Cinema Tycoon Bid — Android Studio project
Application ID / namespace: com.cinematycoonbid.game
App name: Cinema Tycoon Bid

Open this extracted folder in Android Studio and sync Gradle.
This archive, like the original, does not contain a Gradle wrapper. Use Android Studio's configured Gradle, or generate a compatible wrapper if prompted.
Run on a phone/emulator and check industry selection, Tollywood bidding, saves, and Back navigation.
Build > Generate Signed Bundle / APK > Android App Bundle to generate your .aab.
Use your own upload keystore; never share its password or private key.

index.html: industry selection screen.
game.html: shared playable game for every industry.
industries.js: editable new-industry rosters and fictional balance attributes.
tollywood.html: compatibility redirect to Tollywood mode.
All six industries plus Mix & Match are playable. Tollywood and Bollywood have 50 per category; the other industries have 25 directors, 25 actors and 25 actresses.
Mix & Match interleaves and deduplicates all rosters by person name per role; pool size selectors expose the full merged list.
Each industry has isolated saves. Telugu save keys are retained. Shared photos are cached by name.
All modes use the same fictional INR game economy and scoring formula; attributes are starter game balance, not industry revenue predictions. Telugu-specific pair bonuses have been removed for consistent rules.
UI language remains English. Industry selection is not interface translation.
Games are device-local, without live multiplayer synchronization.

Changes: app label, namespace/applicationId, Kotlin package/path, theme and project name; new industry landing screen and return link.
The changed application ID installs separately from the previous test app, so its local saved games do not transfer automatically.
No signed .aab is included. Android compilation must be completed in Android Studio.

Verification: automated JavaScript execution exercised all seven modes, bidding, awards, movie returns, pool limits, deduplication and save/reload. Android build and visual device testing remain to be done in Android Studio. Native JavaScript alert/confirmation handlers included for bid messages, corrections and reset.

Large-group update:
- 2–50 players.
- 1–6 independently auctioned contracts per person, locked after the first bid.
- Player-count changes automatically increase contracts and pools to at least 3 entries per player per category, up to the available pool.
- A contract can be used once; two contracts for the same artist cannot fill the same role in one movie.
- Saved games without contract settings retain their current auction ownership.
- Base roster counts are unchanged; extra contracts increase opportunities, not the number of unique artists.

Large-group automated checks passed for 20, 30 and 50 players in every mode, including independent contract ownership, duplicate-cast guards and save/reload.

Bollywood expanded to 50 directors, 50 actors and 50 actresses, including classic and contemporary artists. Mixed rosters include these additions automatically.

Setup flow: Game settings → Player details → Start Game → Auction. Bidding is blocked until Start Game. Existing auctions resume directly; editing names preserves bids. Reset returns to game settings.

Movie studio flow: Finish auction → one player studio at a time → final results. Player dropdown and Previous/Next navigate without scrolling past other producers. Each studio includes the existing best-combination suggestions. Results show winner (or current leader while unfinished), every player's game worth, profit/loss, budget left, spend and box office. Worth = 100 Cr + profit. All data remains on the same device; separate studios are not online multiplayer sessions.

Surprise Auction: choose Manual or Surprise during setup. Surprise mode randomly reveals one unsold, unrevealed contract across all three roles. Future roster dropdowns/gallery are hidden. Award or pass before revealing again; a lot with bids cannot be passed. Passed lots return only through the explicit end-of-round button. Current reveal and seen lots persist on reload. Small CSS/SVG visuals include a clapperboard, spotlight mystery card, reveal animation and result star; reduced-motion preference is respected.

Settings-first launch: every industry opens Game Settings on a fresh page load, even with a saved auction. Resume Game restores the last auction/studio/results stage. Start New Game asks before clearing this industry’s progress and unlocks setup.

Bidding screen: Player purchases & budget selector shows purchased directors, actors and actresses with individual auction prices, total purchases, production spending and remaining budget. Selecting a player here never places a bid.

New-game defaults: Random/Surprise Auction and Automatic pool sizing. Automatic sets 3 entries per player per category and supplies enough contracts (e.g. 8 players = 24 of each; 20 players = 60 of each). Manual pool sizing unlocks quantity and contract selectors. Existing saved auction styles and active-game quantities remain unchanged.
