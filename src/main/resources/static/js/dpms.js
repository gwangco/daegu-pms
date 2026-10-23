/*
 * D-PMS 공통 화면 스크립트
 * CSP(script-src 'self') 때문에 HTML 안의 onclick/onsubmit/<script> 를 쓰지 않고,
 * data-* 속성을 보고 이 파일에서 동작을 연결한다.
 */
(function () {
    'use strict';

    // ---------- 페이징 (eGov PaginationRenderer 가 그린 data-page 링크) ----------
    // eGov 표준 화면의 fn_egov_link_page(pageNo) 와 같은 역할: 검색 폼의 pageIndex 만 바꿔 다시 조회한다.
    document.addEventListener('click', function (event) {
        var link = event.target.closest('.pagination [data-page]');
        if (!link) {
            return;
        }
        event.preventDefault();
        var page = link.getAttribute('data-page');
        var form = document.getElementById('searchForm');
        if (form && form.elements.pageIndex) {
            form.elements.pageIndex.value = page;
            form.submit();   // submit() 은 submit 이벤트를 발생시키지 않으므로 아래 1페이지 초기화와 충돌하지 않는다
            return;
        }
        var url = new URL(window.location.href);
        url.searchParams.set('pageIndex', page);
        window.location.assign(url.toString());
    });

    // ---------- 폼: 확인 창, 검색 시 1페이지부터, 중복 제출 방지 ----------
    document.addEventListener('submit', function (event) {
        var form = event.target;
        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        if (form.id === 'searchForm' && form.elements.pageIndex) {
            form.elements.pageIndex.value = 1;
        }
        if (!form.hasAttribute('data-allow-resubmit')) {
            form.querySelectorAll('button[type="submit"]').forEach(function (button) {
                button.disabled = true;
                button.setAttribute('aria-busy', 'true');
            });
        }
    });

    // 뒤로 가기로 돌아온 화면(bfcache)에서는 비활성화한 버튼을 되돌린다
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('button[aria-busy="true"]').forEach(function (button) {
            button.disabled = false;
            button.removeAttribute('aria-busy');
        });
    });

    // ---------- 입력칸 아래 안내 문구 (금액 한글 단위, 첨부 파일 크기) ----------
    // 안내 문단을 입력칸 바로 뒤에 한 번만 만들고 aria-describedby 로 연결한다. 값이 바뀌면 화면낭독기가 읽는다(aria-live)
    function hintFor(input, className) {
        var id = input.id + 'Hint';
        var hint = document.getElementById(id);
        if (!hint) {
            hint = document.createElement('p');
            hint.id = id;
            hint.className = className;
            hint.setAttribute('aria-live', 'polite');
            input.insertAdjacentElement('afterend', hint);
            var describedBy = input.getAttribute('aria-describedby');
            input.setAttribute('aria-describedby', describedBy ? describedBy + ' ' + id : id);
        }
        return hint;
    }

    function showHint(hint, text, warning) {
        hint.textContent = text;
        hint.classList.toggle('is-warning', Boolean(warning));
    }

    // 150000000 → "1억 5,000만원", 16500000 → "1,650만원"
    function koreanMoney(value) {
        var n = Math.abs(Math.trunc(value));
        var parts = [];
        if (Math.floor(n / 100000000) > 0) {
            parts.push(Math.floor(n / 100000000).toLocaleString('ko-KR') + '억');
        }
        if (Math.floor((n % 100000000) / 10000) > 0) {
            parts.push(Math.floor((n % 100000000) / 10000).toLocaleString('ko-KR') + '만');
        }
        if (n % 10000 > 0) {
            parts.push((n % 10000).toLocaleString('ko-KR'));
        }
        return parts.length ? (value < 0 ? '-' : '') + parts.join(' ') + '원' : '0원';
    }

    // data-money: 금액을 한글 단위로 읽어 주고, max 를 넘거나 0 보다 작으면 경고 (서버가 다시 검증하므로 안내용)
    function updateMoney(input) {
        var value = parseInt(input.value, 10);
        var hint = hintFor(input, 'money-hint');
        if (isNaN(value)) {
            showHint(hint, '', false);
            return;
        }
        var max = parseInt(input.getAttribute('max'), 10);
        if (!isNaN(max) && value > max) {
            showHint(hint, koreanMoney(value) + ' — 최대 ' + koreanMoney(max) + '을 넘었습니다', true);
        } else if (value < 0 && input.hasAttribute('data-money-negative')) {
            showHint(hint, input.getAttribute('data-money-negative'), true);
        } else {
            showHint(hint, koreanMoney(value), false);
        }
    }

    // ---------- 자동 계산 (서버에서 다시 계산하므로 화면 표시는 안내용) ----------
    function numberValue(id) {
        var el = document.getElementById(id);
        var value = el ? parseInt(el.value, 10) : NaN;
        return isNaN(value) ? 0 : value;
    }

    // 계산에 쓰는 칸이 모두 비어 있으면 결과 칸도 비워 둔다 (아무것도 안 넣었는데 0 이 보이지 않게)
    function allEmpty(ids) {
        return ids.every(function (id) {
            var el = document.getElementById(id);
            return !el || el.value === '';
        });
    }

    var calculators = {
        'self-funding': function () {
            var target = document.getElementById('selfFunding');
            if (target) {
                // 음수도 그대로 보여 주고 경고한다 (예전에는 0 으로 바꿔 입력 실수가 가려졌다)
                target.value = allEmpty(['totalBudget', 'requestGrant']) ? '' : numberValue('totalBudget') - numberValue('requestGrant');
                updateMoney(target);
            }
        },
        'vat': function (input) {
            var target = document.getElementById(input.getAttribute('data-vat-target'));
            var supply = parseInt(input.value, 10);
            if (target) {
                target.value = isNaN(supply) ? '' : Math.floor(supply / 10);   // 서버와 같이 원 단위 미만 절사
            }
            var hint = hintFor(input, 'money-hint');
            if (isNaN(supply)) {
                showHint(hint, '', false);
                return;
            }
            var total = supply + Math.floor(supply / 10);
            var remaining = parseInt(input.getAttribute('data-remaining'), 10);
            if (!isNaN(remaining) && total > remaining) {
                showHint(hint, '부가세 포함 ' + koreanMoney(total) + ' — 등록 가능 잔액 ' + koreanMoney(remaining) + '을 넘었습니다', true);
            } else {
                showHint(hint, '부가세 포함 ' + koreanMoney(total), false);
            }
        },
        'score-total': function () {
            var target = document.getElementById('totalScore');
            if (target) {
                target.value = allEmpty(['quantitativeScore', 'qualitativeScore'])
                        ? '' : numberValue('quantitativeScore') + numberValue('qualitativeScore');
            }
        }
    };

    function recalculate(input) {
        var name = input.getAttribute('data-calc');
        if (name && calculators[name]) {
            calculators[name](input);
        }
        if (input.hasAttribute('data-money')) {
            updateMoney(input);
        }
    }

    document.addEventListener('input', function (event) {
        recalculate(event.target);
    });

    document.querySelectorAll('[data-calc], [data-money]').forEach(recalculate);

    // ---------- 첨부 파일 안내: 이름과 크기, 용량·개수를 넘으면 올리기 전에 알린다 ----------
    // data-max-mb: 파일당 최대 MB, data-max-count: 최대 개수, data-existing-count: 이미 올린 파일 수 (수정 화면)
    // 서버 표시(AttachedFile.sizeLabel)와 같은 규칙: 1MB 이상은 MB 소수 한 자리, 미만은 KB
    function sizeLabel(bytes) {
        return bytes >= 1024 * 1024 ? (bytes / 1024 / 1024).toFixed(1) + 'MB' : Math.max(1, Math.round(bytes / 1024)) + 'KB';
    }

    function updateFileHint(input) {
        var hint = hintFor(input, 'file-hint');
        var files = Array.prototype.slice.call(input.files || []);
        if (!files.length) {
            showHint(hint, '', false);
            return;
        }
        var names = files.map(function (file) {
            return file.name + ' (' + sizeLabel(file.size) + ')';
        });
        var text = files.length === 1 ? '선택한 파일: ' + names[0] : '선택한 파일 ' + files.length + '개: ' + names.join(', ');

        var maxMb = Number(input.getAttribute('data-max-mb'));
        var tooBig = files.filter(function (file) {
            return file.size / 1024 / 1024 > maxMb;
        });
        if (tooBig.length) {
            showHint(hint, text + ' — ' + maxMb + 'MB를 넘는 파일은 접수되지 않습니다. 파일을 줄여 다시 선택하세요.', true);
            return;
        }
        if (input.hasAttribute('data-max-count')) {
            // 이미 올린 파일 중 "삭제"에 표시한 만큼 자리가 생긴다
            var deleting = input.form ? input.form.querySelectorAll('[name="deleteFileSn"]:checked').length : 0;
            var room = Number(input.getAttribute('data-max-count')) - Number(input.getAttribute('data-existing-count') || 0) + deleting;
            if (files.length > room) {
                showHint(hint, text + ' — 지금은 ' + Math.max(0, room) + '개까지 더 올릴 수 있습니다.', true);
                return;
            }
        }
        showHint(hint, text, false);
    }

    document.addEventListener('change', function (event) {
        var input = event.target;
        if (input.type === 'file' && input.hasAttribute('data-max-mb')) {
            updateFileHint(input);
        } else if (input.name === 'deleteFileSn' && input.form) {
            // 삭제 표시를 바꾸면 더 올릴 수 있는 개수가 달라지므로 안내를 다시 계산한다
            input.form.querySelectorAll('input[type="file"][data-max-mb]').forEach(updateFileHint);
        }
    });

    // ---------- 데모 계정 빠른 입력 (로그인 화면) ----------
    document.querySelectorAll('[data-demo-user]').forEach(function (button) {
        button.addEventListener('click', function () {
            document.getElementById('userId').value = button.getAttribute('data-demo-user');
            document.getElementById('password').value = button.getAttribute('data-demo-password');
            document.getElementById('password').focus();
        });
    });

    // ---------- 이전 화면 ----------
    document.querySelectorAll('[data-action="back"]').forEach(function (button) {
        button.addEventListener('click', function () {
            window.history.back();
        });
    });

    // ---------- 모달 폼 채우기 (정산 보완 재요청) ----------
    // 버튼의 data-fill-* 값을 data-fill-target 모달의 같은 name 입력칸에 넣는다.
    document.querySelectorAll('[data-fill-target]').forEach(function (button) {
        button.addEventListener('click', function () {
            var modal = document.querySelector(button.getAttribute('data-fill-target'));
            if (!modal) {
                return;
            }
            Object.keys(button.dataset).forEach(function (key) {
                if (key.indexOf('fill') !== 0 || key === 'fillTarget') {
                    return;
                }
                var name = key.charAt(4).toLowerCase() + key.slice(5);
                var field = modal.querySelector('[name="' + name + '"]');
                if (field) {
                    field.value = button.dataset[key];
                    field.dispatchEvent(new Event('input', { bubbles: true }));
                }
            });
            var form = modal.querySelector('form');
            if (form && button.getAttribute('data-form-action')) {
                form.setAttribute('action', button.getAttribute('data-form-action'));
            }
        });
    });

    // 모달을 닫으면 입력 내용과 안내 문구를 비운다 (다음에 열 때 이전 값이 남아 다른 건으로 잘못 등록하지 않게)
    document.querySelectorAll('.modal[data-reset-on-close]').forEach(function (modal) {
        modal.addEventListener('hidden.bs.modal', function () {
            modal.querySelectorAll('form').forEach(function (form) {
                form.reset();
                form.querySelectorAll('[data-calc], [data-money]').forEach(recalculate);
                form.querySelectorAll('.file-hint').forEach(function (hint) {
                    showHint(hint, '', false);
                });
            });
        });
    });

    // ---------- 막대 폭 (메인 분야별 예산, 정산 집행률) ----------
    // 금액은 글자로 이미 표시되어 있고, 여기서는 막대 폭만 맞춘다.
    // style 속성 대신 CSSOM(el.style.width)으로 넣으므로 CSP(style-src 'self')에 걸리지 않는다
    document.querySelectorAll('[data-bar-list]').forEach(function (list) {
        var items = list.querySelectorAll('[data-value]');
        var max = 0;
        items.forEach(function (item) {
            max = Math.max(max, Number(item.getAttribute('data-value')) || 0);
        });
        items.forEach(function (item) {
            var fill = item.querySelector('.bar-fill');
            if (fill && max > 0) {
                fill.style.width = ((Number(item.getAttribute('data-value')) || 0) / max * 100) + '%';
            }
        });
    });

    document.querySelectorAll('[data-meter-total]').forEach(function (meter) {
        var total = Number(meter.getAttribute('data-meter-total')) || 0;
        meter.querySelectorAll('[data-amount]').forEach(function (part) {
            var amount = Number(part.getAttribute('data-amount')) || 0;
            part.style.width = total > 0 ? Math.min(100, amount / total * 100) + '%' : '0';
        });
    });
})();
