# Ceritaria Studio Android --- Design Specification

## 1. Design direction

Ceritaria Studio should feel like a professional publishing tool
condensed for a phone: compact, calm, content-first and fast.

Avoid: - giant KPI cards; - excessive gradients/glass effects; -
decorative whitespace that pushes content below the fold; - floating
controls with unclear meaning; - icon-only actions without
labels/tooltips/content descriptions; - "AI dashboard" visual clutter.

## 2. Material baseline

Use Material 3 behavior and accessibility, but customize density and
visual hierarchy for Ceritaria.

### Spacing

Use a 4dp base grid: - 4: micro - 8: tight - 12: compact - 16:
standard - 24: section - 32: major

### Touch targets

Minimum 48x48dp even when the visible icon is 20--24dp.

### Shape

-   small controls: 8dp
-   fields/list containers: 10--12dp
-   large modal/sheet: 16--20dp Do not make every rectangle a giant
    pill.

## 3. Color roles

Use semantic Material color roles, not hardcoded feature colors: -
primary: brand/action; - surface: main background; - surfaceContainer:
grouped content; - error: destructive/error; - tertiary only when it has
semantic purpose.

Status must never rely on color alone.

## 4. Typography

-   Screen title: 22--24sp, strong
-   Section title: 16--18sp
-   Body: 14--16sp
-   Metadata: 12--13sp
-   Buttons: readable, not tiny uppercase

Long series/episode titles may wrap to two lines in lists before
ellipsis.

## 5. Navigation

Bottom bar: - Home - Series - Episodes - Analytics

Top app bar: - current screen title; - context actions; -
account/settings entry.

Editor/detail screens are pushed destinations and do not need the bottom
bar if it competes with form actions.

## 6. Home

Structure:

``` text
Top App Bar: Ceritaria Studio          [avatar]

Good morning / operational label
[+ New Episode] [+ New Series]

Needs attention
- 2 drafts
- 1 failed upload

Recent
EP 06 · Aku Cemburu?     Draft
EP 05 · Perhatian         Published
...

Compact performance
Views 7d | Visitors 7d | Player starts
```

No carousel for operational data. Admin tools should optimize scanning,
not audition for a streaming-service homepage.

## 7. Series list

Row preferred over large card.

``` text
[cover 56x80]  Wajah Kedua
               12 episodes
               Published · Featured        [⋮]
```

Controls: - search bar; - filter chips; - sort menu; - FAB "New Series".

Selected filter state must be obvious.

## 8. Series detail

``` text
[hero/cover]
Wajah Kedua
Published · Featured

[Edit] [Preview]

Synopsis
...

Episodes (12)                 [Reorder]
01  ...
02  ...
```

Destructive action belongs in overflow or lower danger zone, never
beside Edit as an equal primary action.

## 9. Episode editor

Use sections, not one giant undifferentiated form.

1.  Identity
    -   Series
    -   Episode number/order
    -   Title
    -   Slug
2.  Story
    -   Description
    -   Recap
    -   Important moments
3.  Video
    -   Provider
    -   URL/provider identifier
    -   Preview/check
4.  Artwork
    -   Thumbnail
    -   Replace/remove
5.  Publishing
    -   status
    -   SEO fields where supported

Sticky bottom action: `Save Draft` and context-sensitive `Publish`.

Dirty form exit: bottom sheet/dialog: - Keep editing - Discard changes

## 10. Media upload

After selection:

``` text
Thumbnail
Uploading 63%
████████████░░░░
248 MB / 394 MB
[Cancel]
```

For image uploads the transfer may be quick, but the state model is the
same.

Background upload appears in Home "Needs attention" and media/editor
status.

## 11. Status language

Use concise labels: - Draft - Published - Unpublished - Uploading -
Failed - Deleted

Pair status with icon/text. Avoid mysterious dots as the sole
explanation.

## 12. Loading

-   Initial list: skeleton rows.
-   Refresh: pull-to-refresh indicator while preserving existing
    content.
-   Save: button progress.
-   Upload: determinate progress where bytes are known.
-   Do not blank an existing screen merely because refresh is running.

## 13. Empty states

Empty state explains the next useful action.

Example: "No episodes yet. Add the first episode to this series."
Button: `Add episode`

Search empty: "No episodes match 'xyz'." Action: `Clear search`

## 14. Error states

Inline for recoverable page failures: "Couldn't load episodes." `Retry`

Field errors directly below the field.

Global auth failure returns to login with a useful message.

Never display raw stack traces/PostgREST errors to the user.

## 15. Accessibility

-   48dp targets.
-   Content descriptions for meaningful icons/images.
-   Decorative images excluded from accessibility tree.
-   Dynamic font scaling supported.
-   Contrast appropriate for text/action roles.
-   Do not communicate publish status only by hue.
-   Logical TalkBack order.
-   Keyboard/focus behavior should remain usable on tablets/Chromebooks.

## 16. Responsive behavior

Phone portrait is primary.

Width classes: - compact: single pane; - medium: wider content,
constrained editor width; - expanded: optional list/detail split for
tablets.

Do not stretch text forms edge-to-edge across a tablet.

## 17. Motion

Motion communicates state: - navigation; - list insertion/removal; -
progress; - expand/collapse.

Keep transitions short and non-essential. Respect reduced-motion
platform behavior where applicable.

## 18. Video encode & upload UX

Video is a first-class section of the episode editor.

Before selection:

``` text
Video
[ Select video ]
or
Provider: YouTube / Facebook for legacy/external content
Local R2 replacement uses Select video -> Upload -> READY -> Preview -> Attach
```

After local selection:

``` text
Master video
02:34 · 2160×3840 · 30 fps · 1.2 GB

Studio output
MP4 · H.264 · 1080×1920 · AAC

[ Prepare video ]
```

If source already passes compatibility:
`Ready to upload · No re-encode needed`

Encoding:

``` text
Preparing video
63%
████████████░░░░
Keep Ceritaria Studio open for best performance.
[ Cancel ]
```

Uploading:

``` text
Uploading
248 MB / 394 MB · 63%
████████████░░░░
You may leave this screen.
[ Cancel upload ]
```

Verifying:

``` text
Verifying upload
Transfer complete · checking server asset
Existing episode video is still active.
```

Completed:

``` text
Video ready
186 MB · R2
[ Preview ] [ Pasang ke episode ]

After attachment:
Video R2 aktif
[ Preview ] [ Ganti video ]
```

Rules: - Encoding, Uploading, Verifying, and READY must have distinct status labels. - Do
not show "Uploading" while the device is actually encoding. - Upload completion
must not visually imply that verification has completed. - Show source and
expected output before destructive replacement. - Existing episode video
remains active until replacement is fully verified. - Failed upload offers
Retry and keeps valid encoded output when possible. - Resumable/multipart
progress should preserve transferred progress where the backend contract
supports it. - Insufficient local storage must be detected before starting
a large encode when estimable. - A form with no valid active video source cannot be published. A pending replacement does not block an already-valid existing source because the old video remains active until explicit attachment succeeds.
