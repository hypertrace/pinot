module.exports = {
    "prefix": "v",
    "ignoreIssuesWith": [
        "duplicate",
        "wontfix",
        "invalid",
        "help wanted"
    ],
    "ignoreCommitsWith": [
        "closed"
    ],   
    "template": {
        "commit": "- {{message}}",
        "issue": "- [{{text}}]({{url}}) {{name}}"
    },
    "groupBy": {
        "Enhancements:": ["enhancement", "internal"],
        "Bug Fixes:": ["bug"]
    }
};