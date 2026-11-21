# Repository settings to apply (owner only)

These cannot be set from a commit. Nothing here has been run: each command changes
settings on `github.com/adityamparikh/jev-support-desk` and needs the repo owner. State
read on 2026-11-20 with read-only `gh api` calls: repo is public, default branch `main`,
`main` is not protected, Dependabot security updates are enabled, secret scanning and
push protection are disabled.

Apply them after `.github/workflows/ci.yml` has run at least once on GitHub (push the
`aine-readiness` branch and open a pull request), because a required status check has to
match a check name GitHub has already seen: `Build and test` and
`Secret scan (gitleaks)`.

## 1. Secret scanning and push protection

Closes the gap gitleaks in CI leaves: push protection rejects a key before it reaches
GitHub at all, rather than failing a check after the push.

```bash
gh api -X PATCH repos/adityamparikh/jev-support-desk \
  -f 'security_and_analysis[secret_scanning][status]=enabled' \
  -f 'security_and_analysis[secret_scanning_push_protection][status]=enabled' \
  -f 'security_and_analysis[secret_scanning_non_provider_patterns][status]=enabled' \
  -f 'security_and_analysis[secret_scanning_validity_checks][status]=enabled'
```

Check: `gh api repos/adityamparikh/jev-support-desk --jq .security_and_analysis`

## 2. Dependabot alerts

Security updates are already on; make sure alerts are too (the weekly version updates come
from `.github/dependabot.yml`).

```bash
gh api -X PUT repos/adityamparikh/jev-support-desk/vulnerability-alerts
```

Check: `gh api repos/adityamparikh/jev-support-desk/vulnerability-alerts` (204 = enabled)

## 3. Protect `main`: changes land through a pull request with CI green

Requires both CI jobs to pass, the branch to be up to date, linear history, and no force
pushes or deletion. `required_approving_review_count` is 0 because this is a solo repo
(a required review would block the owner's own PRs); raise it to 1 when a second
contributor joins. `enforce_admins` is true so the owner's direct pushes go through CI
too; set it to false if that is too strict.

```bash
gh api -X PUT repos/adityamparikh/jev-support-desk/branches/main/protection \
  --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["Build and test", "Secret scan (gitleaks)"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
JSON
```

Check: `gh api repos/adityamparikh/jev-support-desk/branches/main/protection --jq '.required_status_checks.contexts'`

Undo: `gh api -X DELETE repos/adityamparikh/jev-support-desk/branches/main/protection`

## Not needed

- Deployment environments with required reviewers: nothing in this repo deploys, and the
  CI workflow uses no secrets beyond the automatic `GITHUB_TOKEN`.
