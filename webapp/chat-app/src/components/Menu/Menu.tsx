import React from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {useModal} from '../../hooks/useModal';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faCog, faHome, faSignInAlt, faSignOutAlt} from '@fortawesome/free-solid-svg-icons';
import {ThemeMenu} from "./ThemeMenu";
import {WebSocketMenu} from "./WebSocketMenu";
import {RootState} from "@store/index";

const MenuContainer = styled.div`
    display: flex;
    justify-content: space-between;
    padding: ${({theme}) => theme.sizing.spacing.sm};
    background-color: ${({theme}) => theme.colors.surface};
    border-bottom: 1px solid ${({theme}) => theme.colors.border};
`;

const ToolbarLeft = styled.div`
    display: flex;
    gap: ${({theme}) => theme.sizing.spacing.md};
`;

const Dropdown = styled.div`
    color: ${({theme}) => theme.colors.text.primary};
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-decoration: none;
    cursor: pointer;
    position: relative;

    &:hover {
        background-color: ${({theme}) => theme.colors.primary};
        color: white;

    }
`;

const DropButton = styled.a`
    color: ${({theme}) => theme.colors.text.primary};
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-decoration: none;
    cursor: pointer;

    &:hover {
        background-color: ${({theme}) => theme.colors.primary};
        color: white;
    }
`;

const DropdownContent = styled.div`
    display: none;
    position: absolute;
    background-color: ${({theme}) => theme.colors.surface};
    min-width: 160px;
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
    z-index: 1;
    top: 100%;
    left: 0;

    ${Dropdown}:hover & {
        display: block;
    }
`;

const DropdownItem = styled.a`
    color: ${({theme}) => theme.colors.text.primary};
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-decoration: none;
    display: block;
    cursor: pointer;

    &:hover {
        background-color: ${({theme}) => theme.colors.primary};
        color: white;
    }
`;

export const Menu: React.FC = () => {
    useSelector((state: RootState) => state.config.websocket);
    const {openModal} = useModal();
    const verboseMode = useSelector((state: RootState) => state.ui.verboseMode);

    const handleMenuClick = (modalType: string) => {
        console.log('[Menu] Opening modal:', modalType);
        openModal(modalType);
        // Verify the action was dispatched
        console.log('[Menu] Modal action dispatched:', {
            type: 'showModal',
            modalType
        });
    };

    const handleLogout = () => {
        console.log('[Menu] User logging out');
    };


    return (
        <MenuContainer>
            <ToolbarLeft>
                <DropButton href="/" onClick={() => console.log('[Menu] Navigating to home')}>
                    <FontAwesomeIcon icon={faHome}/> Home
                </DropButton>

                <Dropdown>
                    <DropButton>App</DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => openModal('sessions')}>Session List</DropdownItem>
                        <DropdownItem onClick={() => console.log('[Menu] Creating new session')}>New</DropdownItem>
                    </DropdownContent>
                </Dropdown>

                <Dropdown>
                    <DropButton onClick={() => console.log('[Menu] Session menu clicked')}>
                        <FontAwesomeIcon icon={faCog}/> Session
                    </DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => handleMenuClick('settings')}>Settings</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('files')}>Files</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('usage')}>Usage</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('threads')}>Threads</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('share')}>Share</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('cancel')}>Cancel</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('delete')}>Delete</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('verbose')}>
                            {verboseMode ? 'Hide Verbose' : 'Show Verbose'}
                        </DropdownItem>
                    </DropdownContent>
                </Dropdown>

                <ThemeMenu/>

                <Dropdown>
                    <DropButton onClick={() => console.log('[Menu] About menu clicked')}>About</DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => handleMenuClick('privacy')}>Privacy Policy</DropdownItem>
                        <DropdownItem onClick={() => handleMenuClick('tos')}>Terms of Service</DropdownItem>
                    </DropdownContent>
                </Dropdown>

                <Dropdown>
                    <DropButton onClick={() => console.log('[Menu] Config menu clicked')}>Config</DropButton>
                    <DropdownContent>
                        <WebSocketMenu/>
                    </DropdownContent>
                </Dropdown>

            </ToolbarLeft>

            <Dropdown>
                <DropButton onClick={() => console.log('[Menu] Login menu clicked')}>
                    <FontAwesomeIcon icon={faSignInAlt}/> Login
                </DropButton>
                <DropdownContent>
                    <DropdownItem onClick={() => handleMenuClick('user-settings')}>Settings</DropdownItem>
                    <DropdownItem onClick={() => handleMenuClick('user-usage')}>Usage</DropdownItem>
                    <DropdownItem onClick={handleLogout}>
                        <FontAwesomeIcon icon={faSignOutAlt}/> Logout
                    </DropdownItem>
                </DropdownContent>
            </Dropdown>
        </MenuContainer>
    );
};
export default Menu;