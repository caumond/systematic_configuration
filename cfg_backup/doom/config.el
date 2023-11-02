;;; $DOOMDIR/config.el -*- lexical-binding: t; -*-

;; Place your private configuration here! Remember, you do not need to run 'doom
;; sync' after modifying this file!

(setq auto-revert-check-vc-info t)

;; How to set a variable (with or without after!) is described here: https://github.com/doomemacs/doomemacs/blob/master/docs/faq.org#changes-to-my-config-arent-taking-effect

;; See `https://docs.projectile.mx/projectile/configuration.html', for details
(after! projectile
  (setq projectile-create-missing-test-files t)
  (setq projectile-auto-discover nil)
  (setq projectile-sort-order 'access-time)
  (add-to-list 'projectile-globally-ignored-directories "~")
  (add-to-list 'projectile-globally-ignored-directories "~/Dev/hephaistox/monorepo")
  (setq projectile-project-root-files-bottom-up (remove ".git"
                                                        projectile-project-root-files-bottom-up)))

;; See `https://github.com/clojure-emacs/clj-refactor.el/wiki#customization' for details
(after! cljr
  (setq cljr-expectations-test-declaration
        "[clojure.test :refer [deftest is testing]]")
  (setq cljr-clojure-test-declaration
        "[clojure.test :refer [deftest is testing]]")
  (setq cljr-cljs-clojure-test-declaration
        "[cljs.test :refer [deftest is testing] :include-macros true]")
  (setq cljr-cljc-clojure-test-declaration
        "#?(:clj [clojure.test :refer [deftest is testing]]
:cljs [cljs.test :refer [deftest is testing] :include-macros true])"))

;; See `https://docs.cider.mx/cider/config/basic_config.html' for details
(after! cider
  (setq cider-auto-mode t)
  (setq cider-default-cljs-repl 'shadow)
  (setq cider-comment-prefix ";")
  (setq cider-repl-pop-to-buffer-on-connect nil)
  (setq cider-comment-continued-prefix ";")
  (setq cider-clojure-cli-global-options "-J-XX:-OmitStackTraceInFastThrow"))

(after! clojure-mode
  (set-formatter! 'zprint '("zprint" "" "-") :modes '(clojure-mode)))

;; Some functionality uses this to identify you, e.g. GPG configuration, email
;; clients, file templates and snippets. It is optional.
(setq user-full-name "Anthony CAUMOND"
      user-mail-address "anthony@caumond.com")

;; Doom exposes five (optional) variables for controlling fonts in Doom:
;;
;; - `doom-font' -- the primary font to use
;; - `doom-variable-pitch-font' -- a non-monospace font (where applicable)
;; - `doom-big-font' -- used for `doom-big-font-mode'; use this for
;;   presentations or streaming.
;; - `doom-unicode-font' -- for unicode glyphs
;; - `doom-serif-font' -- for the `fixed-pitch-serif' face
;;
;; See 'C-h v doom-font' for documentation and more examples of what they
;; accept. For example:
;;
;;(setq doom-font (font-spec :family "Fira Code" :size 12 :weight 'semi-light)
;;      doom-variable-pitch-font (font-spec :family "Fira Sans" :size 13))
;;
;; If you or Emacs can't find your font, use 'M-x describe-font' to look them
;; up, `M-x eval-region' to execute elisp code, and 'M-x doom/reload-font' to
;; refresh your font settings. If Emacs still can't find your font, it likely
;; wasn't installed correctly. Font issues are rarely Doom issues!

;; There are two ways to load a theme. Both assume the theme is installed and
;; available. You can either set `doom-theme' or manually load a theme with the
;; `load-theme' function. This is the default:
(setq doom-theme 'doom-one)
(setq org-duration-format (quote h:mm))

;; This determines the style of line numbers in effect. If set to `nil', line
;; numbers are disabled. For relative line numbers, set this to `relative'.
(setq display-line-numbers-type 'visual)

;; If you use `org' and don't want your org files in the default location below,
;; change `org-directory'. It must be set before org loads!
(setq org-directory "~/Dev/perso/my-notes/todos")

;; Whenever you reconfigure a package, make sure to wrap your config in an
;; `after!' block, otherwise Doom's defaults may override your settings. E.g.
;;
;;   (after! PACKAGE
;;     (setq x y))
;;
;; The exceptions to this rule:
;;
;;   - Setting file/directory variables (like `org-directory')
;;   - Setting variables which explicitly tell you to set them before their
;;     package is loaded (see 'C-h v VARIABLE' to look up their documentation).
;;   - Setting doom variables (which start with 'doom-' or '+').
;;
;; Here are some additional functions/macros that will help you configure Doom.
;;
;; - `load!' for loading external *.el files relative to this one
;; - `use-package!' for configuring packages
;; - `after!' for running code after a package has loaded
;; - `add-load-path!' for adding directories to the `load-path', relative to
;;   this file. Emacs searches the `load-path' when you load packages with
;;   `require' or `use-package'.
;; - `map!' for binding new keys
;;
;; To get information about any of these functions/macros, move the cursor over
;; the highlighted symbol at press 'K' (non-evil users must press 'C-c c k').
;; This will open documentation for it, including demos of how they are used.
;; Alternatively, use `C-h o' to look up a symbol (functions, variables, faces,
;; etc).
;;
;; You can also try 'gd' (or 'C-c c d') to jump to their definition and see how
;; they are implemented.

;; C Control
;; M Option
;; s Command
;; S Shift
;;
;;(map! "C-M-s-e" #'sp-absorb-sexp)
(map! "C-M-s-b" #'sp-add-to-next-sexp)
;;(map! "C-M-s-e" #'sp-add-to-previous-sexp)
(map! "C-M-b" #'sp-backward-barf-sexp)
;; (map! "C-M-b" #'sp-backward-copy-sexp)
;; (map! "C-M-b" #'sp-backward-delete-char)
;; (map! "C-M-b" #'sp-backward-delete-symbol)
;; (map! "C-M-b" #'sp-backward-delete-word)
;; (map! "C-M-b" #'sp-backward-down-sexp)
;; (map! "C-M-b" #'sp-backward-kill-sexp)
;; (map! "C-M-b" #'sp-backward-kill-symbol)
;; (map! "C-M-b" #'sp-backward-kill-word)
;; (map! "C-M-b" #'sp-backward-parallel-sexp)
;; (map! "C-M-b" #'sp-backward-sexp)
;; (map! "C-M-b" #'sp-backward-slurp-sexp)
(map! "C-M-v" #'sp-forward-slurp-sexp)
;; (map! "C-M-b" #'sp-backward-symbol)
;; (map! "C-M-b" #'sp-backward-unwrap-sexp)
;; (map! "C-M-b" #'sp-backward-up-sexp)
;; (map! "C-M-b" #'sp-backward-whitespace)
;; (map! "C-M-b" #'sp-beginning-of-next-sexp)
;; (map! "C-M-b" #'sp-beginning-of-previous-sexp)
;; (map! "C-M-b" #'sp-beginning-of-sexp)
;; (map! "C-M-b" #'sp-change-enclosing)
;; (map! "C-M-b" #'sp-change-inner)
;; (map! "C-M-b" #'sp-char-escaped-p)
;; (map! "C-M-b" #'sp-cheat-sheet) ;; Generate a cheat sheet
;; (map! "C-M-b" #'sp-clone-sexp)
;;
(map! "C-M-;" #'sp-comment)
;; (map! "C-M-b" #'sp-convolute-sexp)
;; (map! "C-M-b" #'sp-copy-sexp)
;; (map! "C-M-b" #'sp-dedent-adjust-sexp)
;; (map! "C-M-b" #'sp-delete-char)
;; (map! "C-M-b" #'sp-delete-pair)
;; (map! "C-M-b" #'sp-delete-region)
;; (map! "C-M-b" #'sp-delete-symbol)
;; (map! "C-M-b" #'sp-delete-word)
;; (map! "C-M-b" #'sp-describe-system) ;; To tell the system data
(map! "C-M-s-e" #'sp-emit-sexp)
;; (map! "C-M-b" #'sp-end-of-next-sexp)
;; (map! "C-M-b" #'sp-end-of-previous-sexp)
;; (map! "C-M-b" #'sp-end-of-sexp)
;; (map! "C-M-b" #'sp-extract-after-sexp)
;; (map! "C-M-b" #'sp-extract-before-sexp)
(map! "C-M-s-b" #'sp-forward-barf-sexp)
;;(map! "C-M-b" #'sp-forward-parallel-sexp)
;;(map! "C-M-f" #'sp-forward-sexp)
;;(map! "C-M-s-b" #'sp-forward-slurp-sexp)
;;(map! "C-M-b" #'sp-forward-symbol)
;; (map! "C-M-b" #'sp-forward-whitespace)
;; (map! "C-M-b" #'sp-highlight-current-sexp)
;; (map! "C-M-b" #'sp-html-next-tag)
;; (map! "C-M-b" #'sp-html-post-handler)
;; (map! "C-M-b" #'sp-html-previous-tag)
;; (map! "C-M-b" #'sp-indent-adjust-sexp)
;; (map! "C-M-b" #'sp-indent-defun)
(map! "C-M-j" #'sp-join-sexp)
;; (map! "C-M-b" #'sp-kill-hybrid-sexp)
;; (map! "C-M-b" #'sp-kill-region)
(map! "C-k" #'sp-kill-sexp)
;; (map! "C-M-b" #'sp-kill-symbol)
;; (map! "C-M-b" #'sp-kill-symbol)
;; (map! "C-M-b" #'sp-kill-whole-line)
(map! "C-M-k" #'sp-kill-word)
;; (map! "C-M-b" #'sp-mark-sexp)
;; (map! "C-M-b" #'sp-narrow-to-sexp)
;; (map! "C-M-b" #'sp-newline)
;; (map! "C-M-b" #'sp-next-sexp)
;; (map! "C-M-b" #'sp-previous-sexp)
;; (map! "C-M-b" #'sp-push-hybrid-sexp)
;; (map! "C-M-b" #'sp-prefix-tag-object)
;; (map! "C-M-b" #'sp-prefix-pair-object)
;; (map! "C-M-b" #'sp-prefix-symbol-object)
;; (map! "C-M-b" #'sp-prefix-save-excursion)
;; (map! "C-M-b" #'sp-region-ok-p) ;; Seems to be internal
(map! "C-M-s-r" #'sp-raise-sexp)
;; (map! "C-M-b" #'sp-rewrap-sexp)
;; (map! "C-M-b" #'sp-select-next-thing)
;; (map! "C-M-b" #'sp-select-previous-thing)
;; (map! "C-M-b" #'sp-select-next-thing-exchange)
;; (map! "C-M-b" #'sp-select-previous-thing-exchange)
;; (map! "C-M-b" #'sp-skip-backward-to-symbol)
;; (map! "C-M-b" #'sp-skip-forward-to-symbol)
;; (map! "C-M-b" #'sp-splice-sexp)
;; (map! "C-M-b" #'sp-transpose-hybrid-sexp)
;; (map! "C-M-b" #'sp-transpose-sexp)
;; (map! "C-M-b" #'sp-unwrap-sexp)
;; (map! "C-M-b" #'sp-up-sexp)
;; (map! "C-M-b" #'sp-update-local-pairs)
;; (map! "C-M-b" #'sp-use-paredit-bindings)
;; (map! "C-M-b" #'sp-use-smartparens-bindings)
;; (map! "C-M-b" #'sp-use-textmode-stringlike-parser-p)
;; (map! "C-M-b" #'sp-wrap)
(map! "C-M-:" #'sp-wrap-cancel)
(map! "C-M-è" #'sp-wrap-curly)
(map! "C-M-(" #'sp-wrap-round)
(map! "C-M-§" #'sp-wrap-square)
(map! "C-f" #'format-all-buffer)
