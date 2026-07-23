licensing-commercialization-strategy.md

# Dual Licensing Strategy

## Overview
This project is available under **two licenses**:

1. **GPL v3** — Open source license (community/non-commercial)
2. **Commercial License** — Proprietary license for enterprises (fee-based)

---

## GPL v3 (Open Source License)

**For**: Developers, researchers, open-source projects, educational use
**Cost**: Free
**Requirements**:
- Source code must remain open
- Improvements/derivatives must be GPL v3
- Proper attribution required
- No warranty (as-is)

**File**: `LICENSE` (root directory)
**Header in source files**:
```
/*
 * Server Control Framework - [filename]
 * Copyright (c) [year] [your-name/organization]
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
```

---

## Commercial License

**For**: Enterprises, proprietary software, closed-source derivatives
**Cost**: TBD (negotiated per customer)
**Benefits**:
- Proprietary modifications allowed
- No copyleft requirement
- Commercial support optional
- Liability terms negotiated
- Private forks permitted

**How it works**:
- Customer licenses code under **Commercial License Agreement**
- Independent from GPL v3 terms
- Typically includes:
  - Right to use in proprietary products
  - Right to modify without open-sourcing
  - Optional support/maintenance SLA
  - Custom modifications/features

---

## Repository Structure

```
/
├── LICENSE                 # GPL v3 license text
├── LICENSE.COMMERCIAL      # Commercial license template
├── CONTRIBUTING.md         # Contribution guidelines
├── README.md              # Project overview
├── COMMERCIAL-LICENSE.md  # How to get commercial license
└── [source code with GPL v3 headers]
```

---

## Key Files to Create

### 1. `LICENSE` (GPL v3 text)
- Download from https://www.gnu.org/licenses/gpl-3.0.txt
- Place in root directory

### 2. `LICENSE.COMMERCIAL`
Template commercial license agreement defining:
- Grant of license
- Permitted uses (proprietary, modifications, etc.)
- Payment terms
- Support/maintenance (optional)
- Limitation of liability
- Term and termination

### 3. `COMMERCIAL-LICENSE.md`
Customer-facing page:
```markdown
# Commercial License

This project is available under a dual-license model:

## When You Need a Commercial License

- [ ] Want to use in proprietary software
- [ ] Need closed-source modifications
- [ ] Require commercial support
- [ ] Operating in regulated sector (defence, healthcare)

## How to Request

Contact: [your-email]
Website: [your-website]

Include:
- Organization name
- Use case
- Number of developers/servers
- Support requirements
```

### 4. `CONTRIBUTING.md`
Update to note:
```markdown
# Contributing

By contributing, you agree your contributions are under GPL v3.
Contributions may be relicensed under commercial license terms.

[Standard contribution guidelines...]
```

---

## Copyright & Attribution

### Option A: Single Copyright Holder
```
Copyright (c) 2026 [Your Name/Organization]
Available under GPL v3 and Commercial License
```

### Option B: Multi-Contributor
```
Copyright (c) 2026 Contributors
Available under GPL v3 and Commercial License
Each contributor retains copyright to their contributions.
```

---

## Commercial License Mechanics

### One-Time Purchase Model
- Customer pays once
- Perpetual license to version purchased
- Updates/upgrades are separate

### Subscription Model
- Monthly/annual fee
- Includes updates, patches, support
- License terminates on non-payment

### Hybrid Model
- GPL v3 available free
- Commercial + Support = annual fee
- Most common for SaaS/infrastructure tools

---

## Implementation Checklist

- [ ] Get GPL v3 license text → `LICENSE`
- [ ] Create commercial license template → `LICENSE.COMMERCIAL`
- [ ] Create customer-facing commercial info → `COMMERCIAL-LICENSE.md`
- [ ] Update CONTRIBUTING.md with dual-license note
- [ ] Add GPL v3 header to all source files
- [ ] Create commercial contact info/process
- [ ] Document copyright holder(s)
- [ ] Add license selector to README
- [ ] Update website/docs about licensing

---

## README Example

Add to top of README:

```markdown
# Server Control Framework

[Description...]

## Licensing

This project is dual-licensed:

- **GPL v3** — Free for open-source projects
  - [License text](LICENSE)
  - Source must remain open
  
- **Commercial License** — For proprietary/enterprise use
  - [Commercial licensing info](COMMERCIAL-LICENSE.md)
  - Contact: [email]

[Rest of README...]
```

---

## Considerations

**Advantages**:
- Encourages open-source contribution (GPL side)
- Generates revenue for maintenance (commercial side)
- Community benefits, enterprise adoption

**Challenges**:
- Must manage contributor copyright clearly
- Requires enforcement (monitor GPL compliance)
- Support overhead on commercial side
- Legal review of commercial license terms

**For Your Case**:
Since this is a critical infrastructure tool:
- Defence/healthcare sectors will want commercial licenses
- Academic/startup research can use GPL v3
- Best of both worlds

---

## Next Steps

1. Choose copyright holder (you, organization, etc.)
2. Decide on commercial model (one-time, subscription, hybrid)
3. Create license files (use templates above)
4. Add headers to all source files
5. Update repository with licensing info
6. Create commercial contact/sales process

