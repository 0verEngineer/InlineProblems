<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# InlineProblems Changelog

## [Unreleased]
### Added
- Regex and glob support in the problem filter list: an entry starting with `re:` is a regular expression, an entry containing `*` or `?` is a glob, everything else still matches the beginning of the problem text ([#94](https://github.com/0verEngineer/InlineProblems/issues/94))
- Problems are redrawn when the color scheme or the look and feel changes, so they follow an automatic switch between light and dark theme ([#61](https://github.com/0verEngineer/InlineProblems/issues/61))
- The problem line length offset can be configured in the settings, the setting existed but was never read

### Changed
- Platform baseline raised from 2021.2.4 to 2025.1, since-build 251, sources target Java 21
- Build migrated to the IntelliJ Platform Gradle Plugin 2.x and Gradle 9.7.1, so the project builds with current JDKs again
- Large files got considerably faster ([#96](https://github.com/0verEngineer/InlineProblems/issues/96)). Measured on a file with 1268 problems: a full daemon run went from ~5000 inlay removals plus ~5000 additions down to none, a single edit from ~2000 inlay operations down to 3, and the initial draw from 149 ms to 81 ms
- The periodic scan only covers the visible editors and runs every 10 seconds instead of every 2
- The intention popup is opened directly instead of through the action system, every ActionUtil entry point for that is deprecated as of 2025.3
- The active problem listener is stored as an enum instead of an int whose meaning depended on the order of the settings combo box
- The IntelliJ Plugin Verifier runs in the CI again, it had silently been replaced by the plugin structure check
- UI test setup ported from runIdeForUiTests to testIdeUi and the IntelliJ Starter framework
- .idea is no longer tracked

### Fixed
- The HighlightProblemListener never reported anything, and "Show only highest severity per line" removed the problems instead of drawing them, both because of an inverted check
- Severities configured under "Additional severities" for infos were rendered as weak warnings
- With more than one project open, every scan removed and redrew all problems of the other projects
- "Enable notifications" could not be saved
- "Enable XML unescaping" had no effect
- Clicking a problem label looked the intention action up through the wrong ActionManager, so it only ever worked through its fallback
- A file opened in a split view had its problems removed by the other editor on every scan
- The fallback font was lost while building the label font, so characters the first font does not cover were rendered as boxes ([#58](https://github.com/0verEngineer/InlineProblems/issues/58))
- Drawn inlays and highlighters are removed by reference instead of being searched by hash code, the likely cause of a gutter icon or a fixed problem staying visible ([#44](https://github.com/0verEngineer/InlineProblems/issues/44), [#38](https://github.com/0verEngineer/InlineProblems/issues/38))
- Problems of closed editors and projects are cleaned up right away, a listener disposable leaked per opened file and the periodic scan kept running after a plugin unload
- The Unity project detection left its file readers open
- Colors with a low alpha value were serialized incorrectly
- Three settings checkboxes were not initialized from the stored state

## [0.5.10]
### Fixed
- Fixed max lines feature 0-ignore logic (problems not appearing)
- Fixed and improved the problem reset and refresh after using actions (keyboard shortcuts) to enable/disable severity levels or the whole plugin functionality

## [0.5.9]
### Changed
- Added a max lines feature to exclude big files
- Added a max problems per line feature

## [0.5.8]
### Fixed
- Fixed settings not opening with non default locales

## [0.5.7]
### Changed
- Added feature to enable and disable all problems with a keybind (thanks to khopland)
- Added feature to hover and click in a problem to get the fix context window (thanks to khopland)

## [0.5.6]
### Changed
- Added 2 settings options to disable HTML stripping and XML unescaping
### Fixed
- Generics in error messages were stripped because of the HTML stripping

## [0.5.5]
### Changed
- Try to use unlimited until-build for future IDEA Releases

## [0.5.4]
### Changed
- Use unlimited until-build for future IDEA Releases

## [0.5.3]
### Added
- Ability to blacklist file extensions so that the plugin will not show anything on blacklisted files

## [0.5.2]
### Added
- Compability with 2024.1

## [0.5.1]
### Fixed
- SettingsBundle missing default (Settings not working with non english os language)
- Prevents gutter icon readding when the line of the problem does not exist anymore

## [0.5.0]

### Added
- Gutter icon support (disabled by default)
- Notification when a Unity project is opened in Rider, and it switches to the ManualScan listener
- Chinese translation (thanks to kuweiguge)

### Fixed
- AlreadyDisposedException in HighlightProblemListener

## [0.4.3]

### Added
- Some performance improvements
- Description for the listeners in the settings
- Configurable delay for the manual scan listener
- Support for new EAP versions

### Changed
- MarkupModelListener is now the default listener

### Fixed
- Some possible null pointer exceptions
- invokeLater queuing issue of MarkupModelListener if used with the only one problem per line feature
- Cache of activeProblems is now thread safe
- CustomSeverity bugs that leads to useless problem updates

## [0.4.2]

### Added
- Custom severity values (comma separated list) for the different severity levels

### Fixed
- Possible index out of bounds exception
- Flicker issued with the new only show highest severity per line feature

## [0.4.0] - 2023-04-08

### Added
- Problems are now sorted by severity
- Setting to show only the problem with the highest severity per line
- Bold and italic font styles for problem labels

### Fixed
- Problem label boxes were drawn to high and sometimes a line did not disappear because it was drawn in the line above
- AlreadyDisposedException in HighlightProblemListener

## [0.3.3] - 2023-02-18

### Added
- Font delta setting to decrease the problem label size

### Fixed
- Settings refresh not reliable

## [0.3.2] - 2023-02-04

### Fixed
- Deprecated function usage

## [0.3.1] - 2023-02-04

### Added
- MarkupModelListener for problem collecting
- Reset & rescan of all problems on settings change
- Adding ManualScan per delay of 250ms between finishing of the previous scan and starting a new one for problem collecting
- Support for Unity Engine projects in Rider (it will switch to the ManualScan listener while Unity projects are open)

### Fixed
- SettingsState serialization issue (error on start)
- Settings always modified
- Invalid problems were shown
- Font width calculation wrong and font size change was not working
- Xml and html text in the problem description is now handled

## [0.2.1] - 2023-01-12

### Fixed
- Empty problems shown
- Index out of bounds error
- Versioning

## [0.2.0] - 2023-01-08

### Added
- Option to change between editor and tooltip font
- Fallback font loading
- Option for filled inlay boxes

### Fixed
- Label size calculation
- Spacing between inlay boxes

## [0.1.2] - 2023-01-03

### Added
- Reload after settings change

### Fixed
- Problem filtering
- Removal / flicker of problems in dual pane mode

## [0.1.1] - 2022-12-19

### Changed
- Default settings improved

### Fixed
- Removal of labels
- Duplicated problems shown
- Severity checks fix

## [0.1.0]

### Added
- Initial release with basic functionality

[Unreleased]: https://github.com/0verEngineer/InlineProblems/compare/v0.4.0...HEAD
[0.4.3]: https://github.com/0verEngineer/InlineProblems/compare/v0.4.2...v0.4.3
[0.4.2]: https://github.com/0verEngineer/InlineProblems/compare/v0.4.0...v0.4.2
[0.4.0]: https://github.com/0verEngineer/InlineProblems/compare/v0.3.3...v0.4.0
[0.3.3]: https://github.com/0verEngineer/InlineProblems/compare/0.3.2...v0.3.3
[0.3.2]: https://github.com/0verEngineer/InlineProblems/compare/0.3.1...0.3.2
[0.3.1]: https://github.com/0verEngineer/InlineProblems/compare/0.2.1...0.3.1
[0.2.1]: https://github.com/0verEngineer/InlineProblems/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/0verEngineer/InlineProblems/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/0verEngineer/InlineProblems/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/0verEngineer/InlineProblems/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/0verEngineer/InlineProblems/commits
