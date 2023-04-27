<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# InlineProblems Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/OverEngineer/InlineProblems/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/0verEngineer/InlineProblems/compare/v0.3.3...v0.4.0
[0.3.3]: https://github.com/0verEngineer/InlineProblems/compare/0.3.2...v0.3.3
[0.3.2]: https://github.com/0verEngineer/InlineProblems/compare/0.3.1...0.3.2
[0.3.1]: https://github.com/0verEngineer/InlineProblems/compare/0.2.1...0.3.1
[0.2.1]: https://github.com/0verEngineer/InlineProblems/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/0verEngineer/InlineProblems/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/0verEngineer/InlineProblems/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/0verEngineer/InlineProblems/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/0verEngineer/InlineProblems/commits
